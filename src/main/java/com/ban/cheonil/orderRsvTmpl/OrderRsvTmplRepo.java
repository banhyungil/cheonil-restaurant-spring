package com.ban.cheonil.orderRsvTmpl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ban.cheonil.orderRsvTmpl.entity.OrderRsvTmpl;

public interface OrderRsvTmplRepo
    extends JpaRepository<OrderRsvTmpl, Short>, JpaSpecificationExecutor<OrderRsvTmpl> {

  /**
   * 스케줄러용 — [startT, endT) 범위 rsv_time 인 활성 템플릿 조회.
   *
   * <p>조건: active=true / start_dt ≤ date / (end_dt is null or end_dt ≥ date) / day_types 에 day 포함.
   *
   * <p>native query + 명시적 cast 사용 — JPQL 의 {@code function('array_position', ...)} 은 PG 의
   * polymorphic resolver 가 implicit varchar→day_type cast 를 따라가지 못해 함수 dispatch 실패.
   */
  @Query(
      value =
          """
          select * from m_order_rsv_tmpl t
          where t.active = true
            and t.start_dt <= :date
            and (t.end_dt is null or t.end_dt >= :date)
            and t.rsv_time >= :startT
            and t.rsv_time < :endT
            and (:day)::day_type = ANY(t.day_types)
          """,
      nativeQuery = true)
  List<OrderRsvTmpl> findActiveInWindow(
      @Param("date") LocalDate date,
      @Param("day") String day,
      @Param("startT") LocalTime startT,
      @Param("endT") LocalTime endT);
}
