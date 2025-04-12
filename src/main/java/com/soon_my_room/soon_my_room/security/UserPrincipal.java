package com.soon_my_room.soon_my_room.security;

import com.soon_my_room.soon_my_room.model.User;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** 인증을 위한 사용자 정보를 담는 클래스 User 엔티티와 Spring Security의 UserDetails를 분리 */
@AllArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {

  private final User user;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }


  @Override
  public boolean isEnabled() {
    return user.isActive();
  }

  /** 기본 사용자 정보 조회 */
  public User getUser() {
    return user;
  }
}
