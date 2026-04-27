package com.ban.cheonil.orderRsvTmpl;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplMenuExtRes;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmplMenu;
import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmplMenuId;

public interface OrderRsvTmplMenuRepo
    extends JpaRepository<OrderRsvTmplMenu, OrderRsvTmplMenuId> {

  /** 여러 템플릿의 메뉴 항목 + 메뉴 정보 batch fetch. */
  @Query(
      """
          select new com.ban.cheonil.orderRsvTmpl.dto.OrderRsvTmplMenuExtRes(
              orm.id.menuSeq, orm.id.rsvTmplSeq, orm.price, orm.cnt, m.nm, m.nmS)
          from OrderRsvTmplMenu orm
          join Menu m on m.seq = orm.id.menuSeq
          where orm.id.rsvTmplSeq in :tmplSeqs
          """)
  List<OrderRsvTmplMenuExtRes> findExtsByTmplSeqs(@Param("tmplSeqs") List<Short> tmplSeqs);

  /** 한 템플릿의 모든 메뉴 항목 삭제 (수정/삭제 시 사용). */
  void deleteByIdRsvTmplSeq(Short rsvTmplSeq);
}
