package com.ban.cheonil.orderRsv;

import com.ban.cheonil.orderRsv.dto.OrderRsvMenuExtRes;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenu;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenuId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRsvMenuRepo extends JpaRepository<OrderRsvMenu, OrderRsvMenuId> {

  /**
   * 여러 예약의 메뉴 항목 + 메뉴 정보 batch fetch (ad-hoc Menu join).
   *
   * <p><code>@Param</code>:<code>@Query</code> 사용시에 참조 하기 위해 사용
   */
  @Query(
      """
          select new com.ban.cheonil.orderRsv.dto.OrderRsvMenuExtRes(
              orm.id.menuSeq, orm.id.rsvSeq, orm.price, orm.cnt, m.nm, m.nmS)
          from OrderRsvMenu orm
          join Menu m on m.seq = orm.id.menuSeq
          where orm.id.rsvSeq in :rsvSeqs
          """)
  List<OrderRsvMenuExtRes> findExtsByRsvSeqs(@Param("rsvSeqs") List<Long> rsvSeqs);

  /**
   * 여러 예약 의 메뉴 목록 조회. {@code @EmbeddedId} 라 복합키 경로 (id.rsvSeq) 로 derived query 명명.
   */
  List<OrderRsvMenu> findByIdRsvSeqIn(List<Long> rsvSeqs);

  /** 한 예약의 모든 메뉴 항목 삭제 (수정/삭제 시 사용). */
  void deleteByIdRsvSeq(Long rsvSeq);
}
