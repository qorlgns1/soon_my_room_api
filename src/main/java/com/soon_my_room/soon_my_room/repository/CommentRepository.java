package com.soon_my_room.soon_my_room.repository;

import com.soon_my_room.soon_my_room.model.Comment;
import com.soon_my_room.soon_my_room.model.Post;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

  List<Comment> findByPostOrderByCreatedAtDesc(Post post, Pageable pageable);

  int countByPost(Post post);
}
