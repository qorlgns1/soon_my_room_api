package com.soon_my_room.soon_my_room.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LoginRequestDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LoginRequest {
    @Valid private LoginUser user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {
      @NotBlank(message = "이메일을 입력해주세요.")
      @Email(message = "잘못된 이메일 형식입니다.")
      private String email;

      @NotBlank(message = "비밀번호를 입력해주세요.")
      private String password;
    }
  }
}
