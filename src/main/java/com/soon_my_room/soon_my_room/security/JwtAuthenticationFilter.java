package com.soon_my_room.soon_my_room.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService userDetailsService;

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

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      jwt = authorizationHeader.substring(7);
      try {
        email = jwtUtil.extractEmail(jwt);
      } catch (Exception e) {
        logger.error("JWT token validation failed", e);
      }
    }

    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

      if (jwtUtil.validateToken(jwt, userDetails)) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      log.info("=== 응답 완료: {} {} - 상태: {} ===", method, requestURI, response.getStatus());
    }
  }
}
