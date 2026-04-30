package com.ban.cheonil.common.exception;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** REST API 전역 예외 처리. 컨트롤러에서 throw 된 예외를 일관된 JSON 응답으로 변환. */
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

  /**
   * DB 무결성 제약 위반 → 409. PG SQLState 로 케이스 분기하여 일반화된 한글 메시지 반환 (raw 메시지 노출 X).
   *
   * <ul>
   *   <li>{@code 23505} unique_violation
   *   <li>{@code 23503} foreign_key_violation
   *   <li>{@code 23502} not_null_violation
   * </ul>
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException e) {
    Throwable root = e.getMostSpecificCause();
    String sqlState = (root instanceof SQLException sql) ? sql.getSQLState() : null;
    String message =
        switch (sqlState != null ? sqlState : "") {
          case "23505" -> "이미 존재하는 값입니다.";
          case "23503" -> "다른 데이터가 참조 중이라 처리할 수 없습니다.";
          case "23502" -> "필수 값이 누락되었습니다.";
          default -> "데이터 무결성 제약 위반";
        };
    return body(HttpStatus.CONFLICT, message);
  }

  private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "status", status.value(),
                "message", message,
                "timestamp", OffsetDateTime.now().toString()));
  }
}
