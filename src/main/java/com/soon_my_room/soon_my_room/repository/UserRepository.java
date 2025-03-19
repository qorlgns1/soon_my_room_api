package com.soon_my_room.soon_my_room.repository;

import com.soon_my_room.soon_my_room.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByAccountname(String accountname);

    boolean existsByEmail(String email);

    boolean existsByAccountname(String accountname);
}