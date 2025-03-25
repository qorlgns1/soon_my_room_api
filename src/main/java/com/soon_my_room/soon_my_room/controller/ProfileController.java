package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import com.soon_my_room.soon_my_room.service.FollowService;
import com.soon_my_room.soon_my_room.service.ProfileService;
import com.soon_my_room.soon_my_room.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Profile", description = "프로필 및 팔로우 관련 API")
public class ProfileController {

  private final FollowService followService;
  private final ProfileService profileService;
  private final UserRepository userRepository;
  private final UserService userService;

  @Operation(summary = "프로필 조회", description = "특정 사용자의 프로필을 조회합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "계정이 존재하지 않음")
      })
  @GetMapping("/profile/{accountname}")
  public ResponseEntity<ProfileDTO.ProfileResponse> getProfile(
      @PathVariable String accountname, Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String currentUserEmail = userDetails.getUsername();
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(
                () ->
                    new com.soon_my_room.soon_my_room.exception.ResourceNotFoundException(
                        "사용자를 찾을 수 없습니다."));

    ProfileDTO.ProfileResponse response =
        profileService.getProfile(accountname, currentUser.getId());
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "프로필 수정", description = "사용자 프로필을 수정합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "409", description = "이미 사용중인 계정 ID")
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
  @PutMapping("/user")
  public ResponseEntity<?> updateProfile(
      @Valid @RequestBody UserRequestDTO.UpdateProfileRequest request,
      Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String currentUserEmail = userDetails.getUsername();

    try {
      ProfileDTO.ProfileResponse response =
          userService.updateProfile(currentUserEmail, request.getUser());
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.DuplicateResourceException e) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
          .body(Map.of("message", "이미 사용중이 계정 ID입니다."));
    }
  }

  @Operation(summary = "팔로우", description = "특정 사용자를 팔로우합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "팔로우 성공"),
        @ApiResponse(responseCode = "400", description = "자기 자신을 팔로우할 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "계정이 존재하지 않음")
      })
  @PostMapping("/profile/{accountname}/follow")
  public ResponseEntity<?> followUser(
      @PathVariable String accountname, Authentication authentication) {
    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();
      User currentUser =
          userRepository
              .findByEmail(currentUserEmail)
              .orElseThrow(
                  () ->
                      new com.soon_my_room.soon_my_room.exception.ResourceNotFoundException(
                          "사용자를 찾을 수 없습니다."));

      ProfileDTO.ProfileResponse response =
          followService.followUser(currentUser.getId(), accountname);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(summary = "언팔로우", description = "특정 사용자 팔로우를 취소합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "언팔로우 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "계정이 존재하지 않음")
      })
  @DeleteMapping("/profile/{accountname}/unfollow")
  public ResponseEntity<ProfileDTO.ProfileResponse> unfollowUser(
      @PathVariable String accountname, Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String currentUserEmail = userDetails.getUsername();
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(
                () ->
                    new com.soon_my_room.soon_my_room.exception.ResourceNotFoundException(
                        "사용자를 찾을 수 없습니다."));

    ProfileDTO.ProfileResponse response =
        followService.unfollowUser(currentUser.getId(), accountname);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "팔로워 목록 조회", description = "특정 사용자의 팔로워 목록을 조회합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "계정이 존재하지 않음")
      })
  @GetMapping("/profile/{accountname}/follower")
  public ResponseEntity<List<ProfileDTO.Profile>> getFollowers(
      @PathVariable String accountname,
      @Parameter(description = "페이지당 항목 수") @RequestParam(required = false) Integer limit,
      @Parameter(description = "건너뛸 항목 수") @RequestParam(required = false) Integer skip,
      Authentication authentication) {

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String currentUserEmail = userDetails.getUsername();
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(
                () ->
                    new com.soon_my_room.soon_my_room.exception.ResourceNotFoundException(
                        "사용자를 찾을 수 없습니다."));

    List<ProfileDTO.Profile> followers =
        followService.getFollowerProfiles(accountname, currentUser.getId(), limit, skip);
    return ResponseEntity.ok(followers);
  }

  @Operation(summary = "팔로잉 목록 조회", description = "특정 사용자가 팔로우하는 사용자 목록을 조회합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "계정이 존재하지 않음")
      })
  @GetMapping("/profile/{accountname}/following")
  public ResponseEntity<List<ProfileDTO.Profile>> getFollowing(
      @PathVariable String accountname,
      @Parameter(description = "페이지당 항목 수") @RequestParam(required = false) Integer limit,
      @Parameter(description = "건너뛸 항목 수") @RequestParam(required = false) Integer skip,
      Authentication authentication) {

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String currentUserEmail = userDetails.getUsername();
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(
                () ->
                    new com.soon_my_room.soon_my_room.exception.ResourceNotFoundException(
                        "사용자를 찾을 수 없습니다."));

    List<ProfileDTO.Profile> following =
        followService.getFollowingProfiles(accountname, currentUser.getId(), limit, skip);
    return ResponseEntity.ok(following);
  }
}
