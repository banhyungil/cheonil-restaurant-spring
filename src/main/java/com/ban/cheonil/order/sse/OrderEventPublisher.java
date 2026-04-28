package com.ban.cheonil.order.sse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SLF4J (Simple Logging Facade for Java)
 *
 * <ul>
 *   <li>로깅 API/인터페이스
 * </ul>
 */

/**
 * 주문 SSE 연결 풀 + broadcast.
 *
 * <ul>
 *   <li>{@link #register()} — 새 클라이언트 emitter 발급 + 풀 등록 + 정리 콜백 부착
 *   <li>{@link #broadcast} — 모든 emitter 에 동일 이벤트 push
 *   <li>{@link #heartbeat()} — 15초 주기 comment frame 전송 (idle 연결 끊김 방지)
 * </ul>
 *
 * <p>{@code CopyOnWriteArrayList} 사용 — 동시 register/remove 와 broadcast iterate 가 락 없이 안전.
 */
@Component
@Slf4j
public class OrderEventPublisher {

  /** Spring 기본 30초 타임아웃 회피. 클라이언트가 끊을 때까지 유지. */
  private static final long TIMEOUT_INFINITE = 0L;

  /** Idle proxy 끊김 방지 ping 주기. */
  private static final long HEARTBEAT_INTERVAL_MS = 15_000;

  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  /** 새 클라이언트 연결 등록. */
  public SseEmitter register() {
    SseEmitter emitter = new SseEmitter(TIMEOUT_INFINITE);
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));
    log.debug("SSE registered, total={}", emitters.size());
    return emitter;
  }

  /** 모든 연결된 클라이언트에 broadcast. 실패한 emitter 는 자동 정리. */
  public void broadcast(String eventName, Object payload) {
    for (SseEmitter em : emitters) {
      try {
        em.send(SseEmitter.event().name(eventName).data(payload));
      } catch (IOException e) {
        em.complete(); // onCompletion → pool 자동 정리
      }
    }
  }

  /** 주기적 heartbeat — comment frame 은 클라가 무시. */
  @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
  public void heartbeat() {
    if (emitters.isEmpty()) return;
    for (SseEmitter em : emitters) {
      try {
        em.send(SseEmitter.event().comment("ping"));
      } catch (IOException e) {
        em.complete();
      }
    }
  }
}
