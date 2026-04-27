package com.ban.cheonil.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.order.dto.OrderMenuExtRes;
import com.ban.cheonil.order.entity.OrderMenu;
import com.ban.cheonil.order.entity.OrderMenuId;

public interface OrderMenuRepo extends JpaRepository<OrderMenu, OrderMenuId> {

  /**
   * 여러 주문의 메뉴 항목을 메뉴 정보(nm, nm_s) 와 join 해서 한 번에 조회.
   *
   * <p>t_order_menu 와 m_menu 사이에 JPA 연관관계가 선언돼 있지 않아 ad-hoc {@code join ... on} 사용 (Hibernate 5.1+
   * 지원). 결과를 {@link OrderMenuExtRes} record 의 canonical constructor 로 직접 변환 (constructor
   * expression).
   */
  @Query(
      """
            select new com.ban.cheonil.order.dto.OrderMenuExtRes(
                om.id.menuSeq, om.id.orderSeq, om.price, om.cnt, m.nm, m.nmS)
            from OrderMenu om
            join Menu m on m.seq = om.id.menuSeq
            where om.id.orderSeq in :orderSeqs
            """)
  List<OrderMenuExtRes> findExtsByOrderSeqs(@Param("orderSeqs") List<Long> orderSeqs);

  /**
   * 한 주문의 모든 메뉴 항목 삭제 (주문 수정/삭제 시 사용).
   *
   * <p>derived query — 복합키의 orderSeq 컴포넌트로 매칭. {@code @EmbeddedId id.orderSeq} 경로 표현.
   */
  void deleteByIdOrderSeq(Long orderSeq);
}
