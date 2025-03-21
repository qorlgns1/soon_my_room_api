package com.soon_my_room.soon_my_room.dto;

import com.soon_my_room.soon_my_room.model.Comment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CommentDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CommentRequest {
    @Valid private CommentContent comment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentContent {
      @NotBlank(message = "댓글을 입력해주세요.")
      private String content;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CommentResponse {
    private CommentDetail comment;

    public static CommentResponse fromEntity(Comment comment, ProfileDTO.Profile author) {
      CommentDetail commentDetail =
          CommentDetail.builder()
              .id(comment.getId())
              .content(comment.getContent())
              .createdAt(comment.getCreatedAt())
              .author(author)
              .build();

      return CommentResponse.builder().comment(commentDetail).build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CommentListResponse {
    private List<CommentDetail> comment;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CommentDetail {
    private String id;
    private String content;
    private LocalDateTime createdAt;
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
      private String comment;
    }

    public static ReportResponse fromCommentId(String commentId) {
      Report report = new Report(commentId);
      return ReportResponse.builder().report(report).build();
    }
  }
}
