package com.ban.cheonil.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 활성화.
 *
 * <p>{@link ConcurrentMapCacheManager} — in-memory ConcurrentHashMap. 단일 노드 운영에 충분 (멀티 노드 /
 * 외부 캐시 필요 시 Redis 등으로 교체).
 *
 * <p>Spring Boot 4 부터 cache auto-configuration 이 starter 분리되어 명시적 {@link CacheManager} 빈 선언 필요.
 *
 * <p>현재 캐시 적용:
 *
 * <ul>
 *   <li>{@code menus} — {@link com.ban.cheonil.menu.MenuService#findAll(boolean)}
 *   <li>{@code stores} — {@link com.ban.cheonil.store.StoreService#findAll(boolean)}
 * </ul>
 *
 * 메뉴/매장 mutation (create/update/remove/patchActive) 시 해당 캐시 전체 evict.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  CacheManager cacheManager() {
    // 사용 중인 캐시 이름을 명시 — 알 수 없는 이름 사용 시 즉시 에러로 잡힘.
    return new ConcurrentMapCacheManager("menus", "stores");
  }
}
