package com.ban.cheonil.orderRsv;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.orderRsv.dto.OrderRsvMenuExtRes;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenu;
import com.ban.cheonil.orderRsv.entity.OrderRsvMenuId;

public interface OrderRsvMenuRepo extends JpaRepository<OrderRsvMenu, OrderRsvMenuId> {

  /** 여러 예약의 메뉴 항목 + 메뉴 정보 batch fetch (ad-hoc Menu join). */
  @Query(
      """
          select new com.ban.cheonil.orderRsv.dto.OrderRsvMenuExtRes(
              orm.id.menuSeq, orm.id.rsvSeq, orm.price, orm.cnt, m.nm, m.nmS)
          from OrderRsvMenu orm
          join Menu m on m.seq = orm.id.menuSeq
          where orm.id.rsvSeq in :rsvSeqs
          """)
  List<OrderRsvMenuExtRes> findExtsByRsvSeqs(@Param("rsvSeqs") List<Long> rsvSeqs);

  /** 한 예약의 모든 메뉴 항목 삭제 (수정/삭제 시 사용). */
  void deleteByIdRsvSeq(Long rsvSeq);
}
