package com.soon_my_room.soon_my_room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthResponseDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TokenValidResponse {
    private boolean isValid;
  }
}
