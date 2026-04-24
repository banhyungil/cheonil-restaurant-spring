package com.ban.cheonil.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 모든 {@link RestController} 의 매핑 경로 앞에 {@code /api} 를 자동으로 prefix.
 * <p>
 * 효과 범위는 REST 컨트롤러로 한정 — swagger-ui, 정적 리소스, 액추에이터 경로는 영향 없음.
 * 컨트롤러는 {@code @RequestMapping("/orders")} 처럼 도메인 path 만 선언하면 된다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }
}
