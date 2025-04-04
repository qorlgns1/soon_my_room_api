package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.AuthResponseDTO;
import com.soon_my_room.soon_my_room.dto.LoginRequestDTO;
import com.soon_my_room.soon_my_room.dto.LoginResponseDTO;
import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.exception.JwtAuthenticationException;
import com.soon_my_room.soon_my_room.service.AuthService;
import com.soon_my_room.soon_my_room.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

  private final UserService userService;
  private final AuthService authService;

  @Operation(summary = "회원 가입", description = "새로운 사용자를 등록합니다. 이메일, 비밀번호, 계정명, 사용자명은 필수 입력사항입니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 또는 중복된 이메일/계정명"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
      })
  @PostMapping
  public ResponseEntity<UserResponseDTO.RegisterResponse> registerUser(
      @Parameter(description = "회원가입 정보", required = true) @Valid @RequestBody
          UserRequestDTO.RegisterRequest requestDTO) {

    UserResponseDTO.RegisterResponse response = userService.registerUser(requestDTO.getUser());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "계정ID 검증", description = "회원가입 시 입력한 계정ID가 사용 가능한지 검증합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "검증 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패")
      })
  @PostMapping("/accountnamevalid")
  public ResponseEntity<UserResponseDTO.AccountValidResponse> validateAccountname(
      @Parameter(description = "계정ID 정보", required = true) @Valid @RequestBody
          UserRequestDTO.AccountValidRequest requestDTO) {

    UserResponseDTO.AccountValidResponse response =
        userService.validateAccountname(requestDTO.getUser().getAccountname());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "로그인",
      description =
          "이메일과 비밀번호를 통해 사용자 로그인을 처리합니다.<br>"
              + "응답으로 Access 토큰이 반환되며, Refresh 토큰은 HTTP-Only 쿠키로 자동 설정됩니다.<br>"
              + "Access 토큰이 만료되면 '/api/user/refresh' 엔드포인트를 사용하여 갱신할 수 있습니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
        @ApiResponse(responseCode = "422", description = "이메일 또는 비밀번호가 일치하지 않음")
      })
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> login(
      @Parameter(description = "로그인 정보", required = true) @Valid @RequestBody
          LoginRequestDTO.LoginRequest requestDTO,
      HttpServletResponse response) {
    LoginResponseDTO loginResponse = authService.login(requestDTO.getUser(), response);
    return ResponseEntity.ok(loginResponse);
  }

  // 토큰 갱신 엔드포인트 추가
  @Operation(
      summary = "Access 토큰 갱신",
      description =
          "Refresh 토큰을 사용하여 새로운 Access 토큰을 발급받습니다. "
              + "Refresh 토큰은 쿠키에서 자동으로 읽거나 요청 본문에서 제공할 수 있습니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh 토큰")
      })
  @PostMapping("/refresh")
  public ResponseEntity<LoginResponseDTO> refreshToken(
      @CookieValue(name = "refresh_token", required = false) String refreshToken) {

    if (refreshToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    try {
      LoginResponseDTO response = authService.refreshAccessToken(refreshToken);
      return ResponseEntity.ok(response);
    } catch (JwtAuthenticationException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }
  }

  // 로그아웃 엔드포인트 추가
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(Authentication authentication, HttpServletResponse response) {

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    authService.logout(userDetails.getUsername(), response);

    return ResponseEntity.ok().build();
  }

  @Operation(summary = "이메일 검증", description = "회원가입 시 입력한 이메일이 사용 가능한지 검증합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "검증 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패")
      })
  @PostMapping("/emailvalid")
  public ResponseEntity<UserResponseDTO.EmailValidResponse> validateEmail(
      @Parameter(description = "이메일 정보", required = true) @Valid @RequestBody
          UserRequestDTO.EmailValidRequest requestDTO) {

    UserResponseDTO.EmailValidResponse response =
        userService.validateEmail(requestDTO.getUser().getEmail());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "토큰 검증",
      description = "JWT 토큰의 유효성을 검증합니다.<br>" + "상단 **Authorize** 버튼에 헤더값을 추가하고 테스트 해주세요.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "검증 성공"),
        @ApiResponse(responseCode = "401", description = "인증 헤더가 누락되었거나 형식이 잘못됨")
      })
  @GetMapping("/checktoken")
  public ResponseEntity<AuthResponseDTO.TokenValidResponse> checkToken(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Authorization 헤더가 없거나 비어있는 경우 401 Unauthorized 반환
    if (authHeader == null || authHeader.isEmpty() || !authHeader.startsWith("Bearer ")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(AuthResponseDTO.TokenValidResponse.builder().isValid(false).build());
    }

    // Bearer 접두사 제거
    String token = authHeader.substring(7);

    AuthResponseDTO.TokenValidResponse response = authService.validateToken(token);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "사용자 검색",
      description = "키워드로 사용자를 검색합니다. 이름(username)이나 계정명(accountname)에 검색어가 포함된 사용자를 찾습니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "검색 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
      })
  @GetMapping("/searchuser")
  public ResponseEntity<List<UserResponseDTO.SearchUserResponse>> searchUsers(
      @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword) {

    List<UserResponseDTO.SearchUserResponse> searchResults = userService.searchUsers(keyword);
    return ResponseEntity.ok(searchResults);
  }
}
