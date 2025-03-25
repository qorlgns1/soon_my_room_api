package com.soon_my_room.soon_my_room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponseDTO {
  private String fieldname;
  private String originalname;
  private String encoding;
  private String mimetype;
  private String destination;
  private String filename;
  private String path;
  private String publicUrl; // 이미지 접근 URL
  private String imageSrc; // 이미지 접근 URL
  private String bucketName; // 버킷 이름
  private long size;
  private String error; // 에러 메시지
}
