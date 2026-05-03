package com.ban.cheonil.orderRsvTmpl.scheduler;

import com.ban.cheonil.order.OrderService;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.orderRsv.OrderRsvMenuRepo;
import com.ban.cheonil.orderRsv.OrderRsvRepo;
import com.ban.cheonil.orderRsv.entity.OrderRsv;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenu;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenuId;
import com.ban.cheonil.orderRsv.entity.RsvStatus;
import com.ban.cheonil.orderRsvTmpl.OrderRsvTmplMenuRepo;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmpl;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmplMenu;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 템플릿 → 한 OrderRsv + 메뉴 항목 일체 생성을 **독립 트랜잭션**으로 처리.
 *
 * <p>{@link OrderRsvSchedulerService} 가 batch 안에서 이 빈을 호출하면 매 호출마다 새 트랜잭션이 열림 ({@code
 * REQUIRES_NEW}). 한 템플릿 INSERT 가 실패해도 다른 템플릿의 트랜잭션엔 영향 없음.
 *
 * <p>self-invocation (같은 빈 안의 메서드 호출은 proxy 우회) 회피를 위해 별도 빈으로 분리.
 */
@Service
@RequiredArgsConstructor
public class OrderRsvCreator {

  private final OrderRsvRepo orderRsvRepo;
  private final OrderRsvMenuRepo orderRsvMenuRepo;
  private final OrderRsvTmplMenuRepo tmplMenuRepo;
  private final OrderService orderService;
  private final Clock clock;

  /**
   * 주문 에약 템플릿 을 통한 주문 예약 생성
   *
   * <p>각 주문 생성은 별도의 트랜잭션을 가진다. {@code Propagation.REQUIRES_NEW}
   *
   * <ul>
   *   <li>REQUIRED (기본) 기존 트랜잭션 있으면 참여, 없으면 신규 생성
   *   <li>REQUIRES_NEW 기존 트랜잭션 일시 중단 → 항상 새 트랜잭션
   * </ul>
   *
   * @return true = 새로 생성됨, false = 이미 존재 (skip)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean create(OrderRsvTmpl tmpl, OffsetDateTime rsvAt) {
    if (orderRsvRepo.existsByRsvTmplSeqAndRsvAt(tmpl.getSeq(), rsvAt)) return false;

    OrderRsv rsv = new OrderRsv();
    rsv.setStoreSeq(tmpl.getStoreSeq());
    rsv.setRsvTmplSeq(tmpl.getSeq());
    rsv.setAmount(tmpl.getAmount());
    rsv.setRsvAt(rsvAt);
    rsv.setStatus(RsvStatus.RESERVED);
    OffsetDateTime now = OffsetDateTime.now(clock);
    rsv.setRegAt(now);
    rsv.setModAt(now);
    OrderRsv saved = orderRsvRepo.save(rsv);

    List<OrderRsvTmplMenu> tmplMenus = tmplMenuRepo.findByIdRsvTmplSeq(tmpl.getSeq());
    List<OrderRsvMenu> rsvMenus =
        tmplMenus.stream()
            .map(
                tm -> {
                  OrderRsvMenuId id = new OrderRsvMenuId();
                  id.setMenuSeq(tm.getId().getMenuSeq());
                  id.setRsvSeq(saved.getSeq());

                  OrderRsvMenu m = new OrderRsvMenu();
                  m.setId(id);
                  m.setPrice(tm.getPrice());
                  m.setCnt(tm.getCnt());
                  return m;
                })
            .toList();
    orderRsvMenuRepo.saveAll(rsvMenus);

    // auto_order=true 면 주문(t_order) 도 즉시 생성 + 예약 status=COMPLETED 전환.
    // OrderService.createFromRsv 가 SSE Created 이벤트도 함께 broadcast 하므로 주방 모니터에 즉시 표시됨.
    if (Boolean.TRUE.equals(tmpl.getAutoOrder())) {
      Order order = orderService.createFromRsv(saved, rsvMenus);
      saved.setOrderSeq(order.getSeq());
      saved.setStatus(RsvStatus.COMPLETED);
      saved.setModAt(OffsetDateTime.now(clock));
    }

    return true;
  }
}
