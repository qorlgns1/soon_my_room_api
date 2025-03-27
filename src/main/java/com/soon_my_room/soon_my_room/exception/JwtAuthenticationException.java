package com.soon_my_room.soon_my_room.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/** JWT 인증 관련 예외를 처리하기 위한 커스텀 예외 클래스 */
@Getter
public class JwtAuthenticationException extends AuthenticationException {

  /** JWT 예외 유형을 나타내는 열거형 */
  @Getter
  public enum ErrorType {
    TOKEN_MISSING("JWT 토큰이 제공되지 않았습니다."),
    TOKEN_INVALID_FORMAT("JWT 토큰 형식이 올바르지 않습니다."),
    TOKEN_EXPIRED("JWT 토큰이 만료되었습니다."),
    TOKEN_SIGNATURE_INVALID("JWT 토큰의 서명이 유효하지 않습니다."),
    TOKEN_MALFORMED("JWT 토큰이 손상되었습니다."),
    USER_NOT_FOUND("토큰에 포함된 사용자 정보를 찾을 수 없습니다."),
    INSUFFICIENT_PERMISSIONS("이 작업에 대한 권한이 없습니다."),
    INVALID_TOKEN_CLAIMS("JWT 토큰의 클레임 정보가 올바르지 않습니다."),
    GENERAL_ERROR("JWT 인증 처리 중 오류가 발생했습니다.");

    private final String description;

    ErrorType(String description) {
      this.description = description;
    }
  }

  private final ErrorType errorType;
  private final String details;

  /**
   * 기본 생성자
   *
   * @param message 에러 메시지
   */
  public JwtAuthenticationException(String message) {
    super(message);
    this.errorType = ErrorType.GENERAL_ERROR;
    this.details = null;
  }

  /**
   * 에러 타입을 지정하는 생성자
   *
   * @param errorType 에러 유형
   */
  public JwtAuthenticationException(ErrorType errorType) {
    super(errorType.getDescription());
    this.errorType = errorType;
    this.details = null;
  }

  /**
   * 에러 타입과 상세 정보를 지정하는 생성자
   *
   * @param errorType 에러 유형
   * @param details 추가 상세 정보
   */
  public JwtAuthenticationException(ErrorType errorType, String details) {
    super(errorType.getDescription());
    this.errorType = errorType;
    this.details = details;
  }

  /**
   * 원인 예외를 포함하는 생성자
   *
   * @param message 에러 메시지
   * @param cause 원인 예외
   */
  public JwtAuthenticationException(String message, Throwable cause) {
    super(message, cause);
    this.errorType = ErrorType.GENERAL_ERROR;
    this.details = cause.getMessage();
  }

  /**
   * 에러 타입과 원인 예외를 포함하는 생성자
   *
   * @param errorType 에러 유형
   * @param cause 원인 예외
   */
  public JwtAuthenticationException(ErrorType errorType, Throwable cause) {
    super(errorType.getDescription(), cause);
    this.errorType = errorType;
    this.details = cause.getMessage();
  }

  /**
   * 상세 메시지를 반환
   *
   * @return 상세 메시지 (기본 메시지와 상세 정보 포함)
   */
  @Override
  public String getMessage() {
    if (details != null && !details.isEmpty()) {
      return super.getMessage() + " 상세 정보: " + details;
    }
    return super.getMessage();
  }
}
