package com.soon_my_room.soon_my_room.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soon_my_room.soon_my_room.exception.JwtAuthenticationException;
import com.soon_my_room.soon_my_room.model.User;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    log.debug("Request URI: {}, Method: {}", request.getRequestURI(), request.getMethod());
    // 헤더 정보 로깅
    request
        .getHeaderNames()
        .asIterator()
        .forEachRemaining(
            headerName -> {
              if (!headerName.toLowerCase().equals("authorization")) { // 보안 정보 제외
                log.debug("헤더 {}: {}", headerName, request.getHeader(headerName));
              }
            });

    if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String jwt;
    final String userEmail;

    jwt = authorizationHeader.substring(7);

    // 토큰에서 이메일 추출
    try {
      userEmail = jwtUtil.extractEmail(jwt);
      if (StringUtils.hasText(userEmail)
          && SecurityContextHolder.getContext().getAuthentication() == null) {
        User userDetails = this.userDetailsService.loadUserByUsername(userEmail);
        if (jwtUtil.validateToken(jwt, userDetails)) {
          SecurityContext context = SecurityContextHolder.createEmptyContext();
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          context.setAuthentication(authToken);
          SecurityContextHolder.setContext(context);
          log.debug("{context}=", context);
        }
      }
      filterChain.doFilter(request, response);
    } catch (JwtAuthenticationException jwtAuthenticationException) {
      sendErrorResponse(response, jwtAuthenticationException);
    }
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
}
