package com.soon_my_room.soon_my_room.dto;

import com.soon_my_room.soon_my_room.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
  private UserData user;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserData {
    private String id;
    private String username;
    private String email;
    private String accountname;
    private String intro;
    private String image;
    private String token;
  }

  public static LoginResponseDTO fromEntity(User user, String token) {
    UserData userData =
        UserData.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .accountname(user.getAccountname())
            .intro(user.getIntro())
            .image(user.getImage())
            .token(token)
            .build();

    return LoginResponseDTO.builder().user(userData).build();
  }

  public static LoginResponseDTO fromEntity(User user, String token, String refreshToken) {
    UserData userData =
        UserData.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .accountname(user.getAccountname())
            .intro(user.getIntro())
            .image(user.getImage())
            .token(token)
            .build();

    return LoginResponseDTO.builder().user(userData).build();
  }
}
