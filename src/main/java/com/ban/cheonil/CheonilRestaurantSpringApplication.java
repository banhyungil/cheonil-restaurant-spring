package com.ban.cheonil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ban.cheonil.order.sse.OrderEventPublisher;

/**
 * 1. EnableScheduling 기능
 *
 * <ul>
 *   <li>Spring 이 TaskScheduler 빈을 자동 등록
 *   <li>모든 빈 스캔 → @Scheduled 메서드 발견 * 각 메서드를
 *   <li>스케줄러에 등록 (지정된 주기로) * 백그라운드 스레드에서 주기적 실행
 *   <li>{@link OrderEventPublisher hearbeat} 참조
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class CheonilRestaurantSpringApplication {

  public static void main(String[] args) {
    SpringApplication.run(CheonilRestaurantSpringApplication.class, args);
  }
}
