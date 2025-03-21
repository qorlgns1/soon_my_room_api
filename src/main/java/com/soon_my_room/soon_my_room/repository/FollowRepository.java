package com.soon_my_room.soon_my_room.repository;

import com.soon_my_room.soon_my_room.model.Follow;
import com.soon_my_room.soon_my_room.model.FollowId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {
  List<Follow> findByFollowerId(String followerId);

  List<Follow> findByFollowingId(String followingId);

  long countByFollowerId(String followerId);

  long countByFollowingId(String followingId);

  boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);
}
