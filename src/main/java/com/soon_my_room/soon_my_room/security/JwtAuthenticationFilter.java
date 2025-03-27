package com.soon_my_room.soon_my_room.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soon_my_room.soon_my_room.exception.JwtAuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String authorizationHeader = request.getHeader("Authorization");
    String requestURI = request.getRequestURI();
    String method = request.getMethod();
    String contextPath = request.getContextPath();

    log.info("=== 요청 진입: {} {} ===", method, requestURI);
    log.info("컨텍스트 경로: {}", contextPath);
    log.info("서블릿 경로: {}", request.getServletPath());
    log.info("Remote IP: {}", request.getRemoteAddr());
    log.info("인증 헤더: {}", request.getHeader("Authorization") != null ? "있음" : "없음");

    // 헤더 정보 로깅
    request
        .getHeaderNames()
        .asIterator()
        .forEachRemaining(
            headerName -> {
              if (!headerName.toLowerCase().equals("authorization")) { // 보안 정보 제외
                log.info("헤더 {}: {}", headerName, request.getHeader(headerName));
              }
            });

    // JWT 검증 로직
    String email = null;
    String jwt = null;
    JwtAuthenticationException authException = null;

    try {
      if (authorizationHeader == null) {
        if (isAuthenticatedEndpoint(request)) {
          authException =
              new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_MISSING);
          log.error(authException.getMessage());
        }
      } else if (!authorizationHeader.startsWith("Bearer ")) {
        authException =
            new JwtAuthenticationException(
                JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT,
                "Authorization 헤더는 'Bearer [토큰]' 형식이어야 합니다");
        log.error(authException.getMessage());
      } else {
        jwt = authorizationHeader.substring(7);
        try {
          email = jwtUtil.extractEmail(jwt);
        } catch (Exception e) {
          // JWT 예외 유형을 더 구체적으로 분류
          JwtAuthenticationException.ErrorType errorType;
          if (e.getMessage().contains("expired")) {
            errorType = JwtAuthenticationException.ErrorType.TOKEN_EXPIRED;
          } else if (e.getMessage().contains("signature")) {
            errorType = JwtAuthenticationException.ErrorType.TOKEN_SIGNATURE_INVALID;
          } else if (e.getMessage().contains("malformed")) {
            errorType = JwtAuthenticationException.ErrorType.TOKEN_MALFORMED;
          } else {
            errorType = JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT;
          }

          authException = new JwtAuthenticationException(errorType, e);
          log.error(authException.getMessage(), e);
        }
      }

      if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        try {
          UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

          if (jwtUtil.validateToken(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
          } else {
            authException =
                new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_EXPIRED);
            log.error(authException.getMessage());
          }
        } catch (UsernameNotFoundException e) {
          authException =
              new JwtAuthenticationException(
                  JwtAuthenticationException.ErrorType.USER_NOT_FOUND, e);
          log.error(authException.getMessage(), e);
        }
      }

      // 인증 오류 예외가 있고, 인증이 필요한 경로이면서 아직 필터 체인이 진행되지 않은 경우에만 오류 응답
      if (authException != null
          && isAuthenticatedEndpoint(request)
          && SecurityContextHolder.getContext().getAuthentication() == null) {
        sendErrorResponse(response, authException);
        return; // 필터 체인 중단
      }

      filterChain.doFilter(request, response);
    } catch (Exception e) {
      log.error("JWT 필터에서 예외 발생", e);
      sendErrorResponse(response, "인증 처리 중 오류가 발생했습니다: " + e.getMessage());
    } finally {
      log.info("=== 응답 완료: {} {} - 상태: {} ===", method, requestURI, response.getStatus());
    }
  }

  private boolean isAuthenticatedEndpoint(HttpServletRequest request) {
    String path = request.getRequestURI();

    // 인증이 필요없는 경로 목록
    return !(path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/api/image/")
        || path.startsWith("/api/user")
        || path.equals("/api/user/accountnamevalid")
        || path.equals("/api/user/emailvalid")
        || path.equals("/api/user/login")
        || path.equals("/api/user/checktoken"));
  }

  private void sendErrorResponse(HttpServletResponse response, JwtAuthenticationException exception)
      throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Map<String, Object> errorDetails = new HashMap<>();
    errorDetails.put("status", HttpStatus.FORBIDDEN.value());
    errorDetails.put("error", "Forbidden");
    errorDetails.put("message", exception.getMessage());
    errorDetails.put("errorType", exception.getErrorType().name());

    if (exception.getDetails() != null) {
      errorDetails.put("details", exception.getDetails());
    }

    objectMapper.writeValue(response.getOutputStream(), errorDetails);
  }

  private void sendErrorResponse(HttpServletResponse response, String errorMessage)
      throws IOException {
    JwtAuthenticationException exception = new JwtAuthenticationException(errorMessage);
    sendErrorResponse(response, exception);
  }
}
