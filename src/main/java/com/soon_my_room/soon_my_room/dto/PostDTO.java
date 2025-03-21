package com.soon_my_room.soon_my_room.dto;

import com.soon_my_room.soon_my_room.model.Post;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PostDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PostRequest {
    @Valid private PostContent post;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostContent {
      private String content;
      private String image;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PostResponse {
    private List<PostDetail> post;

    public static PostResponse fromEntity(
        Post post, boolean hearted, int heartCount, int commentCount, ProfileDTO.Profile author) {
      PostDetail postDetail =
          PostDetail.builder()
              .id(post.getId())
              .content(post.getContent())
              .image(post.getImage())
              .createdAt(post.getCreatedAt())
              .updatedAt(post.getUpdatedAt())
              .hearted(hearted)
              .heartCount(heartCount)
              .commentCount(commentCount)
              .author(author)
              .build();

      return PostResponse.builder().post(Collections.singletonList(postDetail)).build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PostListResponse {
    private List<PostDetail> posts;

    public static PostListResponse fromEntities(List<PostDetail> postDetails) {
      return PostListResponse.builder().posts(postDetails).build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PostDetail {
    private String id;
    private String content;
    private String image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hearted;
    private int heartCount;
    private int commentCount;
    private ProfileDTO.Profile author;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReportResponse {
    private Report report;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report {
      private String post;
    }

    public static ReportResponse fromPostId(String postId) {
      Report report = new Report(postId);
      return ReportResponse.builder().report(report).build();
    }
  }
}
