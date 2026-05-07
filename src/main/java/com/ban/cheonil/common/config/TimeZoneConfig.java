package com.ban.cheonil.common.config;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * 비즈니스 TZ (KST) 를 JVM 기본 TZ 로 고정.
 *
 * <p>{@code main()} 진입 직후 {@link #apply()} 호출 — Spring context 가 만들어지기 전에 셋업되어야 {@link
 * java.time.Clock#systemDefaultZone() systemDefaultZone()} 같이 생성 시점에 TZ 를 캡처하는 빈 ({@link
 * ClockConfig}) 도 안전하게 KST 가 박힘.
 *
 * <p>Docker UTC / 다른 TZ 호스트 차이 무관하게 모든 {@link ZoneId#systemDefault() systemDefault()} 호출이 KST 반환.
 */
public final class TimeZoneConfig {

  /** 식당 운영 TZ. 도커 이미지에도 TZ 설정은 했지만. APP 레벨에서 견고하게 설정 */
  public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  private TimeZoneConfig() {}

  public static void apply() {
    TimeZone.setDefault(TimeZone.getTimeZone(BUSINESS_ZONE));
  }
}
