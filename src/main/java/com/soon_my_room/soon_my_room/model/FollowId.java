package com.soon_my_room.soon_my_room.model;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowId implements Serializable {
  private String followerId;
  private String followingId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FollowId followId = (FollowId) o;
    return Objects.equals(followerId, followId.followerId)
        && Objects.equals(followingId, followId.followingId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(followerId, followingId);
  }
}
