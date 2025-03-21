package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.PostDTO;
import com.soon_my_room.soon_my_room.service.HeartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Tag(name = "Heart", description = "좋아요 관련 API")
public class HeartController {

  private final HeartService heartService;

  @Operation(
      summary = "게시글 좋아요",
      description = "특정 게시글에 좋아요를 추가합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "좋아요 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @PostMapping("/{post_id}/heart")
  public ResponseEntity<?> addHeart(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      PostDTO.PostResponse response = heartService.addHeart(postId, currentUserEmail);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "게시글 좋아요 취소",
      description = "특정 게시글의 좋아요를 취소합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @DeleteMapping("/{post_id}/unheart")
  public ResponseEntity<?> removeHeart(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      PostDTO.PostResponse response = heartService.removeHeart(postId, currentUserEmail);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
  }
}
