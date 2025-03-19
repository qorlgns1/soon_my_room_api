package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.exception.DuplicateResourceException;
import com.soon_my_room.soon_my_room.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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
        @ApiResponse(
            responseCode = "201",
            description = "회원가입 성공",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseDTO.RegisterResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "유효성 검증 실패 또는 중복된 이메일/계정명",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  @PostMapping
  public ResponseEntity<?> registerUser(
      @Parameter(description = "회원가입 정보", required = true) @Valid @RequestBody
          UserRequestDTO.RegisterRequest requestDTO,
      BindingResult bindingResult) {
    // 유효성 검사 오류 처리
    if (bindingResult.hasErrors()) {
      Map<String, String> errors = new HashMap<>();
      for (FieldError error : bindingResult.getFieldErrors()) {
        String field = error.getField().replace("user.", "");
        errors.put(field, error.getDefaultMessage());
      }
      return ResponseEntity.badRequest().body(errors);
    }

    try {
      UserResponseDTO.RegisterResponse response = userService.registerUser(requestDTO.getUser());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (DuplicateResourceException e) {
      Map<String, String> error = new HashMap<>();
      if (e.getMessage().contains("이메일")) {
        error.put("email", "이미 가입된 이메일 주소입니다.");
      } else if (e.getMessage().contains("계정")) {
        error.put("accountname", "이미 사용중인 계정 ID입니다.");
      } else {
        error.put("error", e.getMessage());
      }
      return ResponseEntity.badRequest().body(error);
    } catch (Exception e) {
      Map<String, String> error = new HashMap<>();
      error.put("error", "회원가입 중 오류가 발생했습니다.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }
}
