package com.soon_my_room.soon_my_room.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // Bean Validation 예외 처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    return ResponseEntity.badRequest().body(errors);
  }

  // 중복 리소스 예외 처리
  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<Map<String, String>> handleDuplicateResourceException(
      DuplicateResourceException e) {
    Map<String, String> error = new HashMap<>();
    error.put("error", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  // 리소스 없음 예외 처리
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleResourceNotFoundException(
      ResourceNotFoundException e) {
    Map<String, String> error = new HashMap<>();
    error.put("error", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  // 인증 예외 처리
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleAuthenticationException(
      AuthenticationException e) {
    Map<String, Object> error = new HashMap<>();
    error.put("message", "이메일 또는 비밀번호가 일치하지 않습니다.");
    error.put("status", 422);
    return ResponseEntity.status(422).body(error);
  }

  // 일반적인 서버 오류 처리
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "서버 오류가 발생했습니다.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
