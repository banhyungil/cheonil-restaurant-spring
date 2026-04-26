package com.ban.cheonil.common.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * REST API 전역 예외 처리.
 * 컨트롤러에서 throw 된 예외를 일관된 JSON 응답으로 변환.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 리소스를 찾지 못함 → 404. */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 유효하지 않은 상태 전이 등 비즈니스 룰 위반 → 400. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> badState(IllegalStateException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 잘못된 인자 (validation 실패 등) → 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badArgument(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "message", message,
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
