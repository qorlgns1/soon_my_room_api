package com.soon_my_room.soon_my_room.repository;

import com.soon_my_room.soon_my_room.model.Post;
import com.soon_my_room.soon_my_room.model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

  List<Post> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

  List<Post> findByAuthorInOrderByCreatedAtDesc(List<User> authors, Pageable pageable);

  int countByAuthor(User author);
}
