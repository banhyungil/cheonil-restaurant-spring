package com.ban.cheonil.order.sse;

import com.ban.cheonil.order.dto.OrderExtRes;
import com.ban.cheonil.order.dto.OrderStatusChangeRes;

/**
 * 주문 도메인 이벤트 — Spring {@code ApplicationEventPublisher} 로 publish.
 *
 * <p>{@link OrderEventListener} 가 {@code @TransactionalEventListener(AFTER_COMMIT)} 으로 수신해서
 * SSE broadcast 를 트리거. 트랜잭션 롤백 시엔 자동으로 listener 가 호출되지 않아 정합성 보장.
 *
 * <p>sealed interface + record 조합 — 추가 가능한 이벤트 종류를 컴파일러가 추적.
 */
public sealed interface OrderEvent {

  record Created(OrderExtRes payload) implements OrderEvent {}

  record Updated(OrderExtRes payload) implements OrderEvent {}

  record StatusChanged(OrderStatusChangeRes payload) implements OrderEvent {}

  record Removed(Long seq) implements OrderEvent {}
}
