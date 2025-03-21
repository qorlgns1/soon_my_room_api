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
public class HeartId implements Serializable {
  private String userId;
  private String postId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HeartId heartId = (HeartId) o;
    return Objects.equals(userId, heartId.userId) && Objects.equals(postId, heartId.postId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, postId);
  }
}
