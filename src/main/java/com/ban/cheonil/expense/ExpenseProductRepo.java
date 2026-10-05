package com.ban.cheonil.expense;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.expense.entity.ExpenseProduct;

/** 지출 제품 상세 Repository. */
public interface ExpenseProductRepo extends JpaRepository<ExpenseProduct, Long> {

  long countByPrdSeq(Integer prdSeq);

  List<ExpenseProduct> findByExpsSeqInOrderBySeq(Collection<Long> expsSeqs);

  /**
   * 지출의 품목 전체 삭제 — 벌크 delete 로 즉시 실행.
   *
   * <p>파생 deleteBy 는 엔티티 단위 삭제라 flush 때 실행되는데, Hibernate 는 flush 시 insert 를 delete 보다 먼저
   * 처리한다. 그러면 품목 전체 교체(삭제 후 같은 품목 재삽입) 시 uq_expense_product 위반이 난다.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from ExpenseProduct p where p.expsSeq = :expsSeq")
  void deleteByExpsSeq(@Param("expsSeq") Long expsSeq);
}
