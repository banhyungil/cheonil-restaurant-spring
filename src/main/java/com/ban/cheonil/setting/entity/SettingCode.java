package com.ban.cheonil.setting.entity;

/**
 * 시스템 설정 코드.
 *
 * <p>각 코드는 m_setting 의 PK 로 저장됨. 새 코드 추가 시:
 *
 * <ol>
 *   <li>이 enum 에 상수 추가
 *   <li>m_setting 에 default_config seed row INSERT (init/migration 스크립트)
 * </ol>
 */
public enum SettingCode {
  /** 매장 카드/리스트 표시 순서. config: {"order": [storeSeq, ...]}. 누락된 항목은 끝에 append. */
  STORE_ORDER,

  /** 메뉴 표시 순서. config: {"order": [menuSeq, ...]}. */
  MENU_ORDER,

  /** 매장 카테고리 표시 순서. config: {"order": [ctgSeq, ...]}. */
  STORE_CATEGORY_ORDER,

  /** 메뉴 카테고리 표시 순서. config: {"order": [ctgSeq, ...]}. */
  MENU_CATEGORY_ORDER,

  /**
   * 가게 운영시간 — 통계 시간대 분석 (시간대별 매출 / heatmap 등) bucket 범위.
   *
   * <p>config: {@code {"startHour": 9, "endHour": 20}} (양 끝 inclusive). backend 가 직접 소비.
   */
  OPERATING_HOURS,
}
