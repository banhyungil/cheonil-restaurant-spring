package com.ban.cheonil.order.sse;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

/**
 * 주문 도메인 이벤트 → SSE broadcast 어댑터.
 *
 * <p>{@code @TransactionalEventListener(AFTER_COMMIT)} 로 트랜잭션 commit 후에만 broadcast
 * — DB 가 변경 반영된 시점이 보장되고, 롤백 시엔 자동으로 호출되지 않음.
 *
 * <p>이벤트별 payload 매핑:
 * <ul>
 *   <li>Created / Updated → {@code OrderExtRes} 그대로
 *   <li>StatusChanged → {@code OrderStatusChangeRes} 그대로
 *   <li>Removed → {@code {"seq": Long}} (프론트 plan §2 형식)
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OrderEventListener {

  private final OrderEventPublisher publisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(OrderEvent.Created e) {
    publisher.broadcast("order:created", e.payload());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(OrderEvent.Updated e) {
    publisher.broadcast("order:updated", e.payload());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(OrderEvent.StatusChanged e) {
    publisher.broadcast("order:status-changed", e.payload());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(OrderEvent.Removed e) {
    publisher.broadcast("order:removed", Map.of("seq", e.seq()));
  }
}
