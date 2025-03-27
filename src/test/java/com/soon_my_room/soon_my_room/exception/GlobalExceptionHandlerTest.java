package com.soon_my_room.soon_my_room.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @InjectMocks private GlobalExceptionHandler exceptionHandler;

  @Mock private HttpServletRequest request;

  @Mock private MethodArgumentNotValidException methodArgumentNotValidException;

  @Mock private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    when(request.getRequestURI()).thenReturn("/api/test");
  }

  @Test
  @DisplayName("중복 리소스 예외 처리 테스트")
  void handleDuplicateResourceException() {
    // Given
    String errorMessage = "이미 존재하는 리소스입니다.";
    DuplicateResourceException ex = new DuplicateResourceException(errorMessage);

    // When
    ProblemDetail problemDetail = exceptionHandler.handleDuplicateResourceException(ex, request);

    // Then
    assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
    assertEquals(errorMessage, problemDetail.getDetail());
  }

  @Test
  @DisplayName("리소스 없음 예외 처리 테스트")
  void handleResourceNotFoundException() {
    // Given
    String errorMessage = "리소스를 찾을 수 없습니다.";
    ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);

    // When
    ProblemDetail problemDetail = exceptionHandler.handleResourceNotFoundException(ex, request);

    // Then
    assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
    assertEquals(errorMessage, problemDetail.getDetail());
  }

  @Test
  @DisplayName("유효성 검증 예외 처리 테스트")
  void handleValidationExceptions() {
    // Given
    when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);

    FieldError fieldError1 = new FieldError("object", "field1", "필드1 오류");
    FieldError fieldError2 = new FieldError("object", "field2", "필드2 오류");

    when(bindingResult.getAllErrors())
        .thenReturn(java.util.Arrays.asList(fieldError1, fieldError2));

    // When
    ProblemDetail problemDetail =
        exceptionHandler.handleValidationExceptions(methodArgumentNotValidException, request);

    // Then
    assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
    assertEquals("입력값 검증에 실패했습니다", problemDetail.getDetail());

    @SuppressWarnings("unchecked")
    Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
    assertNotNull(errors);
    assertEquals(2, errors.size());
    assertEquals("필드1 오류", errors.get("field1"));
    assertEquals("필드2 오류", errors.get("field2"));
  }

  @Test
  @DisplayName("사용자명 not found 인증 실패 테스트")
  void handleUsernameNotFoundException() {
    // Given
    UsernameNotFoundException ex = new UsernameNotFoundException("사용자를 찾을 수 없습니다.");

    // When
    ProblemDetail problemDetail = exceptionHandler.handleAuthenticationFailures(ex, request);

    // Then
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), problemDetail.getStatus());
    assertEquals("이메일 또는 비밀번호가 일치하지 않습니다.", problemDetail.getDetail());
  }

  @Test
  @DisplayName("잘못된 자격 증명 인증 실패 테스트")
  void handleBadCredentialsException() {
    // Given
    BadCredentialsException ex = new BadCredentialsException("잘못된 비밀번호입니다.");

    // When
    ProblemDetail problemDetail = exceptionHandler.handleAuthenticationFailures(ex, request);

    // Then
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), problemDetail.getStatus());
    assertEquals("이메일 또는 비밀번호가 일치하지 않습니다.", problemDetail.getDetail());
  }

  @Test
  @DisplayName("일반 인증 예외 처리 테스트")
  void handleAuthenticationException() {
    // Given
    AuthenticationException ex = mock(AuthenticationException.class);

    // When
    ProblemDetail problemDetail = exceptionHandler.handleAuthenticationException(ex, request);

    // Then
    assertEquals(HttpStatus.UNAUTHORIZED.value(), problemDetail.getStatus());
    assertEquals("인증에 실패했습니다.", problemDetail.getDetail());
  }

  @Test
  @DisplayName("접근 권한 예외 처리 테스트 - 메시지 있음")
  void handleAccessDeniedExceptionWithMessage() {
    // Given
    String errorMessage = "이 작업에 대한 권한이 없습니다.";
    AccessDeniedException ex = new AccessDeniedException(errorMessage);

    // When
    ProblemDetail problemDetail = exceptionHandler.handleAccessDeniedException(ex, request);

    // Then
    assertEquals(HttpStatus.FORBIDDEN.value(), problemDetail.getStatus());
    assertEquals(errorMessage, problemDetail.getDetail());
  }

  @Test
  @DisplayName("접근 권한 예외 처리 테스트 - 메시지 없음")
  void handleAccessDeniedExceptionWithoutMessage() {
    // Given
    AccessDeniedException ex = new AccessDeniedException("");

    // When
    ProblemDetail problemDetail = exceptionHandler.handleAccessDeniedException(ex, request);

    // Then
    assertEquals(HttpStatus.FORBIDDEN.value(), problemDetail.getStatus());
    assertEquals("이 작업을 수행할 권한이 없습니다. 로그인 정보를 확인하세요.", problemDetail.getDetail());
  }

  @Test
  @DisplayName("JWT 인증 예외 처리 테스트")
  void handleJwtAuthenticationException() {
    // Given
    JwtAuthenticationException ex =
        new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_EXPIRED);

    // When
    ProblemDetail problemDetail = exceptionHandler.handleJwtAuthenticationException(ex, request);

    // Then
    assertEquals(HttpStatus.FORBIDDEN.value(), problemDetail.getStatus());
    assertEquals(
        JwtAuthenticationException.ErrorType.TOKEN_EXPIRED.getDescription(),
        problemDetail.getDetail());
    assertEquals(
        JwtAuthenticationException.ErrorType.TOKEN_EXPIRED.name(),
        problemDetail.getProperties().get("errorType"));
  }

  @Test
  @DisplayName("JWT 인증 예외 처리 테스트 - 상세 정보 포함")
  void handleJwtAuthenticationExceptionWithDetails() {
    // Given
    JwtAuthenticationException ex =
        new JwtAuthenticationException(
            JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT, "토큰 형식이 올바르지 않습니다.");

    // When
    ProblemDetail problemDetail = exceptionHandler.handleJwtAuthenticationException(ex, request);

    // Then
    assertEquals(HttpStatus.FORBIDDEN.value(), problemDetail.getStatus());
    // 메시지 내용이 포함되는지만 확인
    assertTrue(
        problemDetail
            .getDetail()
            .contains(JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT.getDescription()));
    assertEquals(
        JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT.name(),
        problemDetail.getProperties().get("errorType"));
    assertEquals("토큰 형식이 올바르지 않습니다.", problemDetail.getProperties().get("details"));
  }

  @Test
  @DisplayName("메서드 인자 타입 불일치 예외 처리 테스트")
  void handleMethodArgumentTypeMismatch() {
    // Given
    MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
    when(ex.getName()).thenReturn("id");
    when(ex.getValue()).thenReturn("abc");
    when(ex.getRequiredType()).thenReturn((Class) Integer.class);

    // When
    ProblemDetail problemDetail = exceptionHandler.handleMethodArgumentTypeMismatch(ex, request);

    // Then
    assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
    assertEquals(
        "파라미터 'id'의 값 'abc'이(가) 유효하지 않은 형식입니다. 'Integer' 타입이 필요합니다.", problemDetail.getDetail());
  }

  @Test
  @DisplayName("파일 크기 초과 예외 처리 테스트")
  void handleMaxUploadSizeExceededException() {
    // Given
    MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10 * 1024 * 1024);

    // When
    ProblemDetail problemDetail =
        exceptionHandler.handleMaxUploadSizeExceededException(ex, request);

    // Then
    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE.value(), problemDetail.getStatus());
    assertEquals("업로드 파일의 크기가 허용된 최대 크기를 초과했습니다.", problemDetail.getDetail());
  }

  @Test
  @DisplayName("잘못된 인자 예외 처리 테스트")
  void handleIllegalArgumentException() {
    // Given
    String errorMessage = "잘못된 인자가 전달되었습니다.";
    IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

    // When
    ProblemDetail problemDetail = exceptionHandler.handleIllegalArgumentException(ex, request);

    // Then
    assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
    assertEquals(errorMessage, problemDetail.getDetail());
  }

  @Test
  @DisplayName("일반 예외 처리 테스트")
  void handleGeneralException() {
    // Given
    Exception ex = new Exception("일반 예외 발생");

    // When
    ProblemDetail problemDetail = exceptionHandler.handleGeneralException(ex, request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
    assertEquals("서버 오류가 발생했습니다. 나중에 다시 시도하세요.", problemDetail.getDetail());
  }
}
