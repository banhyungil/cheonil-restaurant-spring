package com.ban.cheonil.voiceOrder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ban.cheonil.menu.MenuService;
import com.ban.cheonil.menu.dto.MenuRes;
import com.ban.cheonil.order.OrderService;
import com.ban.cheonil.order.dto.OrderCreateReq;
import com.ban.cheonil.order.dto.OrderExtRes;
import com.ban.cheonil.order.dto.OrderMenuReq;
import com.ban.cheonil.order.dto.OrderRes;
import com.ban.cheonil.speech.AudioNormalizer;
import com.ban.cheonil.speech.SpeechService;
import com.ban.cheonil.store.StoreService;
import com.ban.cheonil.store.dto.StoreRes;
import com.ban.cheonil.voiceOrder.dto.VoiceOrderCreateRes;
import com.ban.cheonil.voiceOrder.dto.VoiceOrderItem;
import com.ban.cheonil.voiceOrder.dto.VoiceOrderRes;
import com.ban.cheonil.voiceOrder.entity.VoiceOrderLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 음성/텍스트 주문 → 구조화 데이터 변환.
 *
 * <p>claude CLI ({@code claude -p}) 를 subprocess 로 호출하여 매장/메뉴 컨텍스트와 함께 사용자 발화를 분석. 응답은 strict JSON
 * 형식 ({@link VoiceOrderRes}) — prompt 에서 schema 명시 + 후처리에서 fence 제거.
 *
 * <p><b>Session 재사용 — 24h TTL</b>
 *
 * <ul>
 *   <li>첫 호출 또는 메뉴/매장 변경 시 → 새 session 생성, 전체 컨텍스트 + 발화 전송
 *   <li>이후 호출 → 같은 session 으로 발화만 전송 ({@code -r <uuid>}) — 토큰 절약 + 응답 ↑
 *   <li>매장/메뉴 변경 자동 감지: 컨텍스트 hash 비교 (캐시 + hash mismatch → 새 session)
 *   <li>TTL 만료 (24h) → 자동 갱신
 * </ul>
 *
 * <p><b>사전 조건</b>:
 *
 * <ul>
 *   <li>Spring 서버 host 에 {@code claude} CLI 설치 + {@code claude login} 된 세션
 *   <li>매장 1대 운영 + 본인 Max 플랜 사용 시나리오에서 적합. production 멀티 노드엔 부적합.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceOrderService {

  /** claude CLI 호출 timeout — 단순 텍스트 변환은 보통 5~10초. */
  private static final long TIMEOUT_SECONDS = 60;

  /** Session 유효 기간 — 24h. 만료 후 새 session 생성하며 컨텍스트 재로드. */
  private static final Duration SESSION_TTL = Duration.ofHours(24);

  private final MenuService menuService;
  private final StoreService storeService;
  private final SpeechService speechService;
  private final AudioNormalizer audioNormalizer;
  private final OrderService orderService;
  private final VoiceOrderLogService logService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // ─── Session 상태 — synchronized parse() 안에서만 mutate ──────────────
  private String sessionId;
  private Instant sessionStartedAt;
  private String contextHash;

  /** 발화 텍스트를 주문 생성 json으로 파싱 claude cli가 수행 */
  public synchronized VoiceOrderRes parseToOrder(String speechText) {
    if (speechText == null || speechText.isBlank()) {
      throw new IllegalArgumentException("text is empty");
    }

    String currentHash = computeContextHash();
    boolean isNewSession = needsNewSession(currentHash);

    String prompt;
    if (isNewSession) {
      sessionId = UUID.randomUUID().toString();
      sessionStartedAt = Instant.now();
      contextHash = currentHash;
      // 첫 세션시 사전 컨텍스트 설정
      prompt = buildFullPrompt(speechText);
      log.info("[voice-order] new session: {}", sessionId);
    } else {
      // 기존 세션 시 텍스트만 전달
      prompt = buildShortPrompt(speechText);
      log.debug("[voice-order] reuse session: {}", sessionId);
    }

    String rawOutput = invokeClaude(prompt, sessionId, isNewSession);
    String json = stripCodeFence(rawOutput);

    try {
      JsonNode node = objectMapper.readTree(json);
      Short storeSeq = readShort(node.get("storeSeq"));
      String cmt = readString(node.get("cmt"));
      List<VoiceOrderItem> menus = readItems(node.get("menus"));
      List<String> unmatched = readStringArray(node.get("unmatched"));
      return new VoiceOrderRes(storeSeq, menus, cmt, unmatched, rawOutput);
    } catch (IOException e) {
      // session 응답 형식이 망가졌을 가능성 — session 무효화 후 재시도하도록 다음 호출 보장
      invalidateSession();
      throw new RuntimeException("claude 응답 JSON 파싱 실패: " + json, e);
    }
  }

  /**
   * 음성 → 주문 생성 (end-to-end).
   *
   * <p>흐름 (선형 if 분기):
   *
   * <ol>
   *   <li>오디오 저장 + log entity 초기화
   *   <li>Whisper STT → parse&validate → valid 면 주문 생성 후 return
   *   <li>invalid 또는 STT 실패면 Google STT → parse&validate → valid 면 주문 생성 후 return
   *   <li>둘 다 invalid 면 Google 의 실패 throw / 둘 다 STT 자체 실패면 합성 예외 throw
   *   <li>finally: 어떤 경로든 log entry 저장
   * </ol>
   *
   * <p>전화 받으며 자동 생성하는 시나리오라 부분 매칭/추측은 절대 통과시키지 않음 — {@link #validate} 의 5-게이트.
   */
  public VoiceOrderCreateRes createOrderFromAudio(MultipartFile audio) {
    AudioPayload original = readAudio(audio);
    VoiceOrderLog entry = initLogEntry(original); // 원본은 디스크/DB 에 그대로 저장 (재생/감사용).

    // STT 호출 시엔 정규화된 WAV 16kHz mono 사용 — 모바일/브라우저 컨테이너 다양성 + MIME mismatch 회피.
    // 정규화 실패 시 (ffmpeg 부재 등) 원본으로 fallback.
    AudioPayload sttPayload;
    try {
      byte[] wav = audioNormalizer.toWav16kMono(original.bytes());
      sttPayload = new AudioPayload(wav, "audio.wav", "audio/wav");
      log.info("[voice-order] audio normalized: {} bytes → {} bytes (wav 16kHz mono)",
          original.bytes().length, wav.length);
    } catch (IOException e) {
      log.warn("[voice-order] audio normalize failed, using original ({})", e.getMessage());
      sttPayload = original;
    }

    try {
      // ─── 1차: Whisper ───────────────────────────────────────
      Optional<String> whisperText = transcribe(sttPayload, SpeechService.Engine.WHISPER, entry);
      if (whisperText.isPresent()) {
        ParseResult r = parseAndValidate(whisperText.get());
        if (r.isValid()) {
          return createOrderFrom(
              r.parsed(), whisperText.get(), SpeechService.Engine.WHISPER, entry);
        }
        log.info("[voice-order] whisper parse/validate invalid ({}), retry google", r.failure().code());
      }

      // ─── 2차: Google ────────────────────────────────────────
      Optional<String> googleText = transcribe(sttPayload, SpeechService.Engine.GOOGLE, entry);
      if (googleText.isPresent()) {
        ParseResult r = parseAndValidate(googleText.get());
        if (r.isValid()) {
          return createOrderFrom(
              r.parsed(), googleText.get(), SpeechService.Engine.GOOGLE, entry);
        }
        // Google parse 실패 — 양쪽 invalid 로 종결.
        log.warn(
            "[voice-order] both engines parse/validate invalid (final={})", r.failure().code());
        entry.setErrorMessage(r.failure().code() + ": " + r.failure().getMessage());
        throw r.failure();
      }

      // 양쪽 STT 자체 실패 — entry.whisper/googleText 에 마커 기록됨.
      String msg = "STT 양쪽 모두 호출 실패 (whisper + google)";
      log.warn("[voice-order] {}", msg);
      entry.setErrorMessage(msg);
      throw new IllegalStateException(msg);
    } finally {
      try {
        logService.save(entry);
      } catch (Exception e) {
        // 로그 저장 실패는 사용자 흐름에 영향 X — 경고만.
        log.warn("[voice-order] failed to save log entry", e);
      }
    }
  }

  // ─── 헬퍼 ──────────────────────────────────────────────────

  /** 오디오 입력 한 번 읽어 두 엔진에 재사용 (MultipartFile 은 stream 이라 반복 안 됨). */
  private record AudioPayload(byte[] bytes, String filename, String contentType) {}

  /** parse + validate 결과 — 양자택일 (예외 throw 대신 if 분기 가능). */
  private record ParseResult(VoiceOrderRes parsed, VoiceOrderValidationException failure) {
    boolean isValid() {
      return failure == null;
    }

    static ParseResult valid(VoiceOrderRes parsed) {
      return new ParseResult(parsed, null);
    }

    static ParseResult invalid(VoiceOrderValidationException e) {
      return new ParseResult(null, e);
    }
  }

  private AudioPayload readAudio(MultipartFile audio) {
    byte[] bytes;
    try {
      bytes = audio.getBytes();
    } catch (IOException e) {
      throw new RuntimeException("audio read failed", e);
    }
    String contentType = audio.getContentType() != null ? audio.getContentType() : "audio/webm";
    String filename =
        audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm";
    return new AudioPayload(bytes, filename, contentType);
  }

  private VoiceOrderLog initLogEntry(AudioPayload p) {
    String audioPath;
    try {
      audioPath = logService.saveAudio(p.bytes(), p.contentType());
    } catch (IOException e) {
      throw new RuntimeException("audio save failed", e);
    }
    VoiceOrderLog entry = new VoiceOrderLog();
    entry.setAudioPath(audioPath);
    entry.setAudioMime(p.contentType());
    entry.setAudioSizeBytes(p.bytes().length);
    return entry;
  }

  /**
   * STT 호출 + entry 에 텍스트 기록. 성공: text Optional, 실패(HTTP/network 등): 마커 기록 후 empty.
   *
   * <p>throw 안 함 — 호출자가 isPresent 로 분기하면 됨. STT 자체 실패는 부수 케이스로 처리.
   */
  private Optional<String> transcribe(
      AudioPayload p, SpeechService.Engine engine, VoiceOrderLog entry) {
    try {
      String text = speechService.transcribe(p.bytes(), p.filename(), p.contentType(), engine).text();
      recordText(entry, engine, text);
      if (text == null || text.isBlank()) {
        log.info("[voice-order] {} returned empty text (audio={} bytes)", engine, p.bytes().length);
        return Optional.empty();
      }
      log.info("[voice-order] {} transcribed: \"{}\"", engine, text);
      return Optional.of(text);
    } catch (RuntimeException e) {
      log.info("[voice-order] {} STT call failed: {}", engine, e.getMessage());
      recordText(entry, engine, "[STT failed: " + e.getClass().getSimpleName() + " " + e.getMessage() + "]");
      return Optional.empty();
    }
  }

  /** parse(claude) + 5-게이트 검증. throw 대신 ParseResult 로 반환. */
  private ParseResult parseAndValidate(String text) {
    try {
      VoiceOrderRes parsed = parseToOrder(text);
      validate(parsed, text);
      return ParseResult.valid(parsed);
    } catch (VoiceOrderValidationException e) {
      return ParseResult.invalid(e);
    }
  }

  /** valid 한 parsed 데이터로 실제 주문 생성 + entry 마무리. */
  private VoiceOrderCreateRes createOrderFrom(
      VoiceOrderRes parsed, String text, SpeechService.Engine engine, VoiceOrderLog entry) {
    OrderCreateReq req = toOrderCreateReq(parsed);
    OrderRes created = orderService.create(req);
    OrderExtRes order = orderService.findExtBySeq(created.seq());
    String confirmation = buildConfirmation(order);

    entry.setEngineUsed(engine.name());
    entry.setFinalText(text);
    entry.setOrderSeq(order.seq());

    log.info(
        "[voice-order] created order {} from \"{}\" (engine={}, storeSeq={}, menus={})",
        order.seq(),
        text,
        engine,
        parsed.storeSeq(),
        parsed.menus().size());
    return new VoiceOrderCreateRes(order, text, confirmation, engine.name());
  }

  private static void recordText(VoiceOrderLog entry, SpeechService.Engine engine, String text) {
    if (engine == SpeechService.Engine.WHISPER) entry.setWhisperText(text);
    else entry.setGoogleText(text);
  }

  /** 5-게이트 검증 — 하나라도 실패 시 즉시 throw. */
  private void validate(VoiceOrderRes parsed, String text) {
    if (parsed.unmatched() != null && !parsed.unmatched().isEmpty()) {
      log.warn(
          "[voice-order] unmatched fragments: input=\"{}\" unmatched={}", text, parsed.unmatched());
      throw new VoiceOrderValidationException(
          VoiceOrderValidationException.Code.UNMATCHED_FRAGMENTS, text, parsed);
    }
    if (parsed.storeSeq() == null) {
      throw new VoiceOrderValidationException(
          VoiceOrderValidationException.Code.STORE_NOT_MATCHED, text, parsed);
    }
    if (parsed.menus() == null || parsed.menus().isEmpty()) {
      throw new VoiceOrderValidationException(
          VoiceOrderValidationException.Code.NO_ITEMS, text, parsed);
    }
    Map<Short, MenuRes> menuBySeq =
        menuService.findAll(false).stream()
            .collect(Collectors.toMap(MenuRes::seq, Function.identity()));
    for (VoiceOrderItem it : parsed.menus()) {
      if (!menuBySeq.containsKey(it.menuSeq())) {
        log.warn(
            "[voice-order] hallucinated menuSeq={} input=\"{}\" parsed={}",
            it.menuSeq(),
            text,
            parsed);
        throw new VoiceOrderValidationException(
            VoiceOrderValidationException.Code.INVALID_MENU, text, parsed);
      }
      if (it.cnt() == null || it.cnt() <= 0) {
        throw new VoiceOrderValidationException(
            VoiceOrderValidationException.Code.INVALID_QUANTITY, text, parsed);
      }
    }
  }

  /** parsed → OrderCreateReq. menu 가격은 활성 메뉴 사전에서 lookup (LLM 가격 추측 방지). */
  private OrderCreateReq toOrderCreateReq(VoiceOrderRes parsed) {
    Map<Short, MenuRes> menuBySeq =
        menuService.findAll(false).stream()
            .collect(Collectors.toMap(MenuRes::seq, Function.identity()));
    List<OrderMenuReq> menus =
        parsed.menus().stream()
            .map(
                it -> {
                  MenuRes m = menuBySeq.get(it.menuSeq());
                  return new OrderMenuReq(it.menuSeq(), m.price(), it.cnt().shortValue());
                })
            .toList();
    return new OrderCreateReq(parsed.storeSeq(), menus, parsed.cmt());
  }

  /** TTS 확인 멘트 — "강남점 양념치킨 2, 콜라 1, 덜맵게 — 주문 완료". */
  private String buildConfirmation(OrderExtRes order) {
    StringBuilder sb = new StringBuilder();
    if (order.storeNm() != null) sb.append(order.storeNm()).append(" ");
    String items =
        order.menus().stream()
            .map(m -> m.menuNm() + " " + m.cnt())
            .collect(Collectors.joining(", "));
    sb.append(items);
    if (order.cmt() != null && !order.cmt().isBlank()) sb.append(", ").append(order.cmt());
    sb.append(" — 주문 완료");
    return sb.toString();
  }

  /** session 무효화 — 다음 호출 시 새로 생성. */
  private void invalidateSession() {
    sessionId = null;
    sessionStartedAt = null;
    contextHash = null;
  }

  /** 새 session 필요 여부 — null / TTL 만료 / 컨텍스트 변경. */
  private boolean needsNewSession(String currentHash) {
    if (sessionId == null) return true;
    if (sessionStartedAt.isBefore(Instant.now().minus(SESSION_TTL))) return true;
    if (!currentHash.equals(contextHash)) return true;
    return false;
  }

  /** 매장/메뉴 컨텍스트의 fingerprint — 변경 감지용. 캐시 hit 라 비용 미미. */
  private String computeContextHash() {
    List<StoreRes> stores = storeService.findAll(false);
    List<MenuRes> menus = menuService.findAll(false);
    StringBuilder sb = new StringBuilder();
    for (StoreRes s : stores) sb.append(s.seq()).append("|").append(s.nm()).append("\n");
    for (MenuRes m : menus)
      sb.append(m.seq())
          .append("|")
          .append(m.nm())
          .append("|")
          .append(m.nmS())
          .append("|")
          .append(m.price())
          .append("\n");
    return Integer.toHexString(sb.toString().hashCode());
  }

  /** claude CLI subprocess 호출. prompt 는 stdin 으로 전달. */
  private String invokeClaude(String prompt, String sessionId, boolean isNewSession) {
    try {
      // 첫 호출: --session-id 로 새 session 생성 / 이어서: -r 로 기존 session resume
      ProcessBuilder pb =
          isNewSession
              ? new ProcessBuilder("claude", "-p", "--session-id", sessionId)
              : new ProcessBuilder("claude", "-p", "-r", sessionId);
      pb.redirectErrorStream(true);
      Process process = pb.start();

      try (OutputStream stdin = process.getOutputStream()) {
        stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
        stdin.flush();
      }

      boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new RuntimeException("claude CLI timeout (" + TIMEOUT_SECONDS + "s)");
      }

      String output =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        // resume 실패 (session 만료 등) 시 무효화 → 다음 호출이 새 session 생성
        if (!isNewSession) invalidateSession();
        throw new RuntimeException("claude CLI exit " + exitCode + ", output: " + output);
      }

      log.debug("[voice-order] claude raw output: {}", output);
      return output;
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("claude CLI invocation failed", e);
    }
  }

  /** 첫 session — 컨텍스트 + schema + 발화 모두 포함. */
  private String buildFullPrompt(String userText) {
    List<StoreRes> stores = storeService.findAll(false);
    List<MenuRes> menus = menuService.findAll(false);

    // 파이프 구분 포맷 — 토큰 효율 ↑. 매장/메뉴 nm 에 `|` 가 포함될 가능성 무시 (이름에 거의 안 씀).
    StringBuilder storesSection = new StringBuilder();
    for (StoreRes s : stores) {
      storesSection.append(s.seq()).append("|").append(s.nm()).append("\n");
    }

    StringBuilder menusSection = new StringBuilder();
    for (MenuRes m : menus) {
      menusSection
          .append(m.seq())
          .append("|")
          .append(m.nm())
          .append("|")
          .append(m.nmS() != null ? m.nmS() : "")
          .append("|")
          .append(m.price())
          .append("\n");
    }

    return """
        목적: 해당 session은 한국어 주문 발화 분석 후 구조화된 JSON 데이터로 변환 용도.

        # 컬럼 사항
        nm: 이름
        nmS: 이름 단축어

        # 매장 목록 (포맷: seq|nm)
        %s
        # 메뉴 목록 (포맷: seq|nm|nmS|price)
        %s
        # 사용자 발화
        "%s"

        # 응답 schema (TypeScript)
        interface VoiceOrderRes {
          storeSeq: number | null   // 매장명 명시되면 매장 seq, 아니면 null
          menus: Array<{ menuSeq: number; cnt: number }>   // 메뉴 + 수량 (필수, 빈 배열 가능)
          cmt: string | null   // 부정/조건/요청사항
          unmatched: string[]   // 매장/메뉴 사전에 매칭 못한 발화 조각 (원문 그대로). 모두 매칭되면 빈 배열
        }

        # 출력 예시
        {"storeSeq":1,"menus":[{"menuSeq":10,"cnt":2},{"menuSeq":12,"cnt":1}],"cmt":"덜맵게","unmatched":[]}
        {"storeSeq":null,"menus":[{"menuSeq":10,"cnt":1}],"cmt":null,"unmatched":["감자튀김"]}

        # 규칙
        - 메뉴명 및 매장명 정확히 일치하지 않아도 가까운 이름으로 매칭
        - 매칭 실패 시: 추론이 안되는 경우 unmatched 에 원문 그대로 기록
        - 이번 session 동안 위 매장/메뉴 목록과 schema/규칙 그대로 유지. 다음 발화부턴 발화만 보냄.
        - JSON 변환 만, 다른 텍스트 절대 출력 금지
        """
        .formatted(storesSection, menusSection, userText.replace("\"", "\\\""));
  }

  /** 이어지는 session — 발화만. 이전 컨텍스트는 session 에 살아있음. */
  private String buildShortPrompt(String userText) {
    return userText.replace("\"", "\\\"");
  }

  /** 응답에 ```json ... ``` fence 가 끼어있을 경우 제거 — schema 위반이지만 방어적으로. */
  private String stripCodeFence(String text) {
    String t = text.trim();
    if (t.startsWith("```")) {
      int firstNewline = t.indexOf('\n');
      if (firstNewline > 0) t = t.substring(firstNewline + 1);
      if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
    }
    return t.trim();
  }

  private Short readShort(JsonNode node) {
    if (node == null || node.isNull()) return null;
    return (short) node.asInt();
  }

  private String readString(JsonNode node) {
    if (node == null || node.isNull()) return null;
    String s = node.asText("").trim();
    return s.isEmpty() ? null : s;
  }

  private List<VoiceOrderItem> readItems(JsonNode node) {
    if (node == null || !node.isArray()) return List.of();
    return node.valueStream()
        .map(
            n ->
                new VoiceOrderItem(
                    readShort(n.get("menuSeq")), n.get("cnt") != null ? n.get("cnt").asInt(1) : 1))
        .filter(it -> it.menuSeq() != null)
        .toList();
  }

  private List<String> readStringArray(JsonNode node) {
    if (node == null || !node.isArray()) return List.of();
    return node.valueStream().map(n -> n.asText("").trim()).filter(s -> !s.isEmpty()).toList();
  }
}
