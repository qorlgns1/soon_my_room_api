package com.soon_my_room.soon_my_room.repository;

import com.soon_my_room.soon_my_room.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

  Optional<User> findByEmail(String email);

  Optional<User> findByAccountname(String accountname);

  boolean existsByEmail(String email);

  boolean existsByAccountname(String accountname);

  // 사용자 검색 메서드 추가
  @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.accountname LIKE %:keyword%")
  List<User> findByUsernameContainingOrAccountnameContaining(@Param("keyword") String keyword);
}
