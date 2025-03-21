package com.soon_my_room.soon_my_room.repository;

import com.soon_my_room.soon_my_room.model.Heart;
import com.soon_my_room.soon_my_room.model.HeartId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeartRepository extends JpaRepository<Heart, HeartId> {

  boolean existsByUserIdAndPostId(String userId, String postId);

  int countByPostId(String postId);

  List<Heart> findByPostId(String postId);
}
