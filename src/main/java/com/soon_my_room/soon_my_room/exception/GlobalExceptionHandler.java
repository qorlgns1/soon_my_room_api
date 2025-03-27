package com.soon_my_room.soon_my_room.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** 애플리케이션 전역의 예외를 처리하는 클래스 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // 예외를 로깅하는 공통 메서드
  private void logException(Exception ex, HttpStatus status, HttpServletRequest request) {
    log.error(
        "Exception: {} | Status: {} | Path: {} | Message: {}",
        ex.getClass().getSimpleName(),
        status.value(),
        request.getRequestURI(),
        ex.getMessage(),
        ex);
  }

  // 중복 리소스 예외 처리
  @ExceptionHandler(DuplicateResourceException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ProblemDetail handleDuplicateResourceException(
      DuplicateResourceException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.CONFLICT, request);
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  // 리소스 없음 예외 처리
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ProblemDetail handleResourceNotFoundException(
      ResourceNotFoundException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.NOT_FOUND, request);
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  // 유효성 검증 예외 처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.BAD_REQUEST, request);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다");

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage =
                  Optional.ofNullable(error.getDefaultMessage()).orElse("유효하지 않은 값");
              errors.put(fieldName, errorMessage);
            });

    problemDetail.setProperty("errors", errors);
    return problemDetail;
  }

  // 인증 관련 예외 그룹 처리
  @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ProblemDetail handleAuthenticationFailures(Exception ex, HttpServletRequest request) {
    logException(ex, HttpStatus.UNPROCESSABLE_ENTITY, request);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.UNPROCESSABLE_ENTITY, "이메일 또는 비밀번호가 일치하지 않습니다.");
  }

  // 일반 인증 예외 처리
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ProblemDetail handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.UNAUTHORIZED, request);
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.");
  }

  // 접근 권한 예외 처리
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ProblemDetail handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.FORBIDDEN, request);
    String detail =
        Optional.ofNullable(ex.getMessage())
            .filter(msg -> !msg.isEmpty())
            .orElse("이 작업을 수행할 권한이 없습니다. 로그인 정보를 확인하세요.");

    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
  }

  // JWT 토큰 예외 처리
  @ExceptionHandler(JwtAuthenticationException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ProblemDetail handleJwtAuthenticationException(
      JwtAuthenticationException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.FORBIDDEN, request);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());

    problemDetail.setProperty("errorType", ex.getErrorType().name());

    if (ex.getDetails() != null) {
      problemDetail.setProperty("details", ex.getDetails());
    }

    return problemDetail;
  }

  // 파라미터 타입 불일치 예외 처리
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.BAD_REQUEST, request);

    String detail =
        String.format(
            "파라미터 '%s'의 값 '%s'이(가) 유효하지 않은 형식입니다. '%s' 타입이 필요합니다.",
            ex.getName(),
            ex.getValue(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "알 수 없음");

    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
  }

  // 파일 크기 초과 예외 처리
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public ProblemDetail handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.PAYLOAD_TOO_LARGE, request);

    return ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYLOAD_TOO_LARGE, "업로드 파일의 크기가 허용된 최대 크기를 초과했습니다.");
  }

  // 잘못된 인자 예외 처리
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    logException(ex, HttpStatus.BAD_REQUEST, request);
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  // 기타 모든 예외 처리
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ProblemDetail handleGeneralException(Exception ex, HttpServletRequest request) {
    logException(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);

    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 나중에 다시 시도하세요.");
  }
}
