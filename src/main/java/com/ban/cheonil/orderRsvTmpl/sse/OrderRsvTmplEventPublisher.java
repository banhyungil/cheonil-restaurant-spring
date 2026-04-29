// package com.ban.cheonil.orderRsvTmpl.sse;
//
// import java.io.IOException;
// import java.util.List;
// import java.util.concurrent.CopyOnWriteArrayList;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
// public class OrderRsvTmplEventPublisher {
//  /** Spring 기본 30초 타임아웃 회피. 클라이언트가 끊을 때까지 유지. */
//  private static final long TIMEOUT_INFINITE = 0L;
//
//  /** Idle proxy 끊김 방지 ping 주기. */
//  private static final long HEARTBEAT_INTERVAL_MS = 15_000;
//
//  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
//
//  //    register
//  public SseEmitter register() {
//    SseEmitter emitter = new SseEmitter(TIMEOUT_INFINITE);
//    emitters.add(emitter);
//    emitter.onCompletion(() -> emitters.remove(emitter));
//    emitter.onTimeout(() -> emitters.remove(emitter));
//    emitter.onError(e -> emitters.remove(emitter));
//
//    return emitter;
//  }
//
//  //    broadcast
//  public void broadcast(String eventName, Object payload) {
//    for (SseEmitter em : emitters) {
//      try {
//        em.send(SseEmitter.event().name(eventName).data(payload));
//      } catch (IOException e) {
//        em.complete();
//      }
//    }
//  }
//
//  // heartbeat구현
//  @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
//  public void heartbeat() {
//    if (emitters.isEmpty()) return;
//
//    for (SseEmitter em : emitters) {
//      try {
//        em.send(SseEmitter.event().comment("ping"));
//      } catch (IOException e) {
//        em.complete();
//      }
//    }
//  }
// }
