package com.soon_my_room.soon_my_room.dto;

import com.soon_my_room.soon_my_room.model.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProfileDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProfileResponse {
    private Profile profile;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Profile {
    private String _id;
    private String username;
    private String accountname;
    private String intro;
    private String image;
    private boolean isfollow;
    private List<String> following;
    private List<String> follower;
    private int followerCount;
    private int followingCount;

    public static Profile fromEntity(
        User user,
        boolean isFollowing,
        List<String> following,
        List<String> followers,
        int followingCount,
        int followerCount) {
      return Profile.builder()
          ._id(user.getId())
          .username(user.getUsername())
          .accountname(user.getAccountname())
          .intro(user.getIntro())
          .image(user.getImage())
          .isfollow(isFollowing)
          .following(following)
          .follower(followers)
          .followerCount(followerCount)
          .followingCount(followingCount)
          .build();
    }
  }
}
