package com.soon_my_room.soon_my_room.dto;

import com.soon_my_room.soon_my_room.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserResponseDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RegisterResponse {
    private String message;
    private UserData user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserData {
      private String _id;
      private String username;
      private String email;
      private String accountname;
      private String intro;
      private String image;
    }

    public static RegisterResponse fromEntity(User user) {
      UserData userData =
          UserData.builder()
              ._id(user.getId())
              .username(user.getUsername())
              .email(user.getEmail())
              .accountname(user.getAccountname())
              .intro(user.getIntro())
              .image(user.getImage())
              .build();

      return RegisterResponse.builder().message("회원가입 성공").user(userData).build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccountValidResponse {
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EmailValidResponse {
    private String message;
  }
}
