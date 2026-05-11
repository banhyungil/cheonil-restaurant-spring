package com.ban.cheonil.speech;

import java.io.IOException;

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

import com.ban.cheonil.speech.dto.SpeechRes;
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

  private final RestTemplate whisperTemplate;
  private final String whisperUrl;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SpeechService(@Value("${whisper.url}") String whisperUrl) {
    // RestTemplate 생성 및 FormHttpMessageConverter 등록
    // multipart/form-data 직렬화를 명시적으로 등록하여 보장
    this.whisperTemplate = new RestTemplate();
    this.whisperTemplate.getMessageConverters().add(0, new FormHttpMessageConverter());
    this.whisperUrl = whisperUrl;
  }

  public SpeechRes transcribe(MultipartFile audio) {
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

    if (bytes.length == 0) {
      throw new IllegalArgumentException("audio file is empty");
    }

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
