package com.soon_my_room.soon_my_room.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserRequestDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RegisterRequest {
    @Valid private User user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
      @NotBlank(message = "필수 입력사항을 입력해주세요.")
      private String username;

      @NotBlank(message = "필수 입력사항을 입력해주세요.")
      @Email(message = "잘못된 이메일 형식입니다.")
      private String email;

      @NotBlank(message = "필수 입력사항을 입력해주세요.")
      @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
      private String password;

      @NotBlank(message = "필수 입력사항을 입력해주세요.")
      @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "영문, 숫자, 밑줄, 마침표만 사용할 수 있습니다.")
      private String accountname;

      private String intro;
      private String image;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccountValidRequest {
    @Valid private AccountValidUser user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountValidUser {
      @NotBlank(message = "계정ID는 필수 입력사항입니다.")
      @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "영문, 숫자, 밑줄, 마침표만 사용할 수 있습니다.")
      private String accountname;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EmailValidRequest {
    @Valid private EmailValidUser user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailValidUser {
      @NotBlank(message = "이메일은 필수 입력사항입니다.")
      @Email(message = "잘못된 이메일 형식입니다.")
      private String email;
    }
  }
}
