package com.ban.cheonil.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 주문-메뉴 복합키.
 * <p>
 * {@code t_order_menu} 테이블은 (menu_seq, order_seq) 두 컬럼이 묶여 PK 이므로
 * 별도 키 클래스로 분리하고 {@link OrderMenu} 에서 {@code @EmbeddedId} 로 참조한다.
 */
@Getter
@Setter
// 복합키 클래스는 equals/hashCode 필수 — JPA 가 row 동일성 판단에 사용.
// 영속 컨텍스트(1차 캐시) 의 key, Map 기반 자료구조 key 로도 쓰이므로 반드시 구현해야 한다.
// Lombok 이 모든 필드 기반으로 자동 생성.
@EqualsAndHashCode
// 이 클래스 자체는 엔티티가 아니라 "다른 엔티티에 포함되는 값 타입" 임을 선언.
// @EmbeddedId (단일 키 컨테이너) 또는 @Embedded (일반 값 타입) 대상이 된다.
@Embeddable
// JPA 스펙상 복합키 클래스는 Serializable 이어야 한다.
// 분산 캐시/세션 직렬화 시 필요.
public class OrderMenuId implements Serializable {
    // 직렬화 버전 식별자 — 클래스 구조 변경 시 호환성 판단용.
    // Java 관행상 Serializable 구현 클래스에 명시. 값 자체는 임의 (JPA Buddy 자동 생성값).
    private static final long serialVersionUID = 921846760410308980L;

    // 필드 = 복합키의 한 컴포넌트. DB 컬럼 menu_seq 매핑.
    // @EmbeddedId 방식에서는 컬럼 매핑을 "이 키 클래스 안에" 둔다 (엔티티가 아닌 여기).
    @NotNull
    @Column(name = "menu_seq", nullable = false)
    private Short menuSeq;

    @NotNull
    @Column(name = "order_seq", nullable = false)
    private Long orderSeq;
}
