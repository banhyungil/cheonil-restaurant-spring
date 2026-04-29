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
   * day_types 는 day_type[] enum array — array_position 으로 멤버십 검사.
   */
  @Query(
      """
      select t from OrderRsvTmpl t
      where t.active = true
        and t.startDt <= :date
        and (t.endDt is null or t.endDt >= :date)
        and t.rsvTime >= :startT
        and t.rsvTime < :endT
        and function('array_position', t.dayTypes, :day) is not null
      """)
  List<OrderRsvTmpl> findActiveInWindow(
      @Param("date") LocalDate date,
      @Param("day") String day,
      @Param("startT") LocalTime startT,
      @Param("endT") LocalTime endT);
}
