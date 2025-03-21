package com.soon_my_room.soon_my_room.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "follows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(FollowId.class)
public class Follow {

  @Id
  @Column(name = "follower_id")
  private String followerId;

  @Id
  @Column(name = "following_id")
  private String followingId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "follower_id", insertable = false, updatable = false)
  private User follower;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "following_id", insertable = false, updatable = false)
  private User following;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
  }
}
