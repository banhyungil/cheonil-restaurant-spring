package com.ban.cheonil.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시간 의존성 주입용 {@link Clock} 빈.
 *
 * <p>{@code OffsetDateTime.now()} / {@code LocalDateTime.now()} 직접 호출 대신 이 Clock 을 주입받아
 * {@code OffsetDateTime.now(clock)} 형태로 사용. 테스트에선 {@link Clock#fixed} 로 시간 고정 가능.
 */
@Configuration
public class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
