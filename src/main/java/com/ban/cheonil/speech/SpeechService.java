package com.ban.cheonil.speech;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.ban.cheonil.menu.MenuService;
import com.ban.cheonil.speech.dto.SpeechRes;
import com.ban.cheonil.store.StoreService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Whisper STT (faster-whisper webservice) 프록시.
 *
 * <p>POST {@code WHISPER_URL}/asr 로 multipart audio 전달, 한국어 전사 결과 텍스트 반환.
 *
 * <p><b>RestClient 대신 RestTemplate 을 쓰는 이유</b> — Spring Boot 4 / Framework 7 의 RestClient 에서
 * multipart {@code HttpEntity} part 전송 시 일관되게 422 가 발생 (FormHttpMessageConverter 명시 등록해도 동일).
 * RestTemplate 은 같은 페이로드 정상 전송. 환경/API 안정화 후 RestClient 로 변경 검토.
 *
 * <p>응답 Content-Type 이 `text/plain` 이지만 본문은 JSON — String 으로 받아 직접 파싱.
 */
@Service
public class SpeechService {

  /** STT 엔진 선택 — Whisper (self-host, 기본) / Google Cloud STT (fallback). */
  public enum Engine {
    WHISPER,
    GOOGLE
  }

  private final RestTemplate whisperTemplate;
  private final String whisperUrl;
  private final GoogleSpeechClient googleSpeechClient;
  private final MenuService menuService;
  private final StoreService storeService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SpeechService(
      @Value("${whisper.url}") String whisperUrl,
      GoogleSpeechClient googleSpeechClient,
      MenuService menuService,
      StoreService storeService) {
    // RestTemplate 생성 및 FormHttpMessageConverter 등록
    // multipart/form-data 직렬화를 명시적으로 등록하여 보장
    this.whisperTemplate = new RestTemplate();
    this.whisperTemplate.getMessageConverters().add(0, new FormHttpMessageConverter());
    this.whisperUrl = whisperUrl;
    this.googleSpeechClient = googleSpeechClient;
    this.menuService = menuService;
    this.storeService = storeService;
  }

  /**
   * 인식 정확도 boost 용 도메인 어휘 — active 매장명 + 메뉴명(+단축명).
   *
   * <p>Google STT 는 {@code speechContexts.phrases}, Whisper 는 {@code initial_prompt} 로 매핑. 둘 다 매장
   * 컨텍스트 단어 인식률을 명확히 향상시킴.
   */
  private List<String> buildHints() {
    List<String> hints = new ArrayList<>();
    storeService
        .findAll(false)
        .forEach(
            s -> {
              if (StringUtils.hasText(s.nm())) hints.add(s.nm());
            });
    // nm, nmS둘다 개별 요소로 hints에 등록
    menuService
        .findAll(false)
        .forEach(
            m -> {
              Stream.of(m.nm(), m.nmS()).filter(StringUtils::hasText).forEach(hints::add);
            });
    return hints;
  }

  /** MultipartFile 입력. 기본 엔진 Whisper. */
  public SpeechRes transcribe(MultipartFile audio) {
    return transcribe(audio, Engine.WHISPER);
  }

  /** MultipartFile 입력 + 엔진 지정. byte[] 한 번 읽어 오버로드에 위임. */
  public SpeechRes transcribe(MultipartFile audio, Engine engine) {
    String contentType =
        StringUtils.hasText(audio.getContentType()) ? audio.getContentType() : "audio/webm";
    String filename =
        StringUtils.hasText(audio.getOriginalFilename())
            ? audio.getOriginalFilename()
            : "audio.webm";

    byte[] bytes;
    try {
      bytes = audio.getBytes();
    } catch (IOException e) {
      throw new RuntimeException("audio file read failed", e);
    }
    return transcribe(bytes, filename, contentType, engine);
  }

  /**
   * byte[] 입력. retry orchestration 에서 같은 오디오로 여러 엔진 호출할 때 사용 — MultipartFile 은 한 번만 읽을 수 있음.
   *
   * <p>매장/메뉴 hints 를 자동 빌드해 두 엔진에 동일하게 전달 — 인식률 향상.
   */
  public SpeechRes transcribe(byte[] bytes, String filename, String contentType, Engine engine) {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("audio file is empty");
    }
    return switch (engine) {
      // Whisper 는 initial_prompt 토큰 한도(~224) 가 작아 매장/메뉴 ~300개 다 못 담음 → hint 미적용.
      // Google 만 speechContexts.phrases 로 정확도 향상.
      case WHISPER -> transcribeWhisper(bytes, filename, contentType);
      case GOOGLE -> new SpeechRes(googleSpeechClient.transcribe(bytes, contentType, buildHints()));
    };
  }

  private SpeechRes transcribeWhisper(byte[] bytes, String filename, String contentType) {
    // ByteArrayResource 직접 사용 - getFilename() override로 파일명 전달
    ByteArrayResource resource =
        new ByteArrayResource(bytes) {
          @Override
          public String getFilename() {
            return filename;
          }
        };

    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(MediaType.parseMediaType(contentType));
    HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(resource, partHeaders);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("audio_file", filePart);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    String url = whisperUrl + "/asr?language=ko&task=transcribe&output=json";
    String raw;
    try {
      raw = whisperTemplate.postForObject(url, requestEntity, String.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 422) {
        throw new RuntimeException("Whisper rejected multipart payload (422)", e);
      }
      throw e;
    }

    String text = extractText(raw);
    return new SpeechRes(text);
  }

  private String extractText(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }

    String trimmed = raw.trim();
    if (trimmed.startsWith("{")) {
      try {
        JsonNode node = objectMapper.readTree(trimmed);
        JsonNode textNode = node.get("text");
        if (textNode != null && !textNode.isNull()) {
          return textNode.asText("").trim();
        }
      } catch (IOException ignored) {
        // JSON 파싱 실패 시 plain text 로 간주.
      }
    }
    return trimmed;
  }
}
