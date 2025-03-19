package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

  private final UserService userService;

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
}
