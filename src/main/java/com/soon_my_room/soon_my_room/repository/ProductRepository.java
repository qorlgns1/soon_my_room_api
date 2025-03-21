package com.soon_my_room.soon_my_room.repository;

import com.soon_my_room.soon_my_room.model.Product;
import com.soon_my_room.soon_my_room.model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

  List<Product> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

  int countByAuthor(User author);
}
