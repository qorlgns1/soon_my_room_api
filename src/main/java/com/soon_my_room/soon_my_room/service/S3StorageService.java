package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.ImageResponseDTO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  @Value("${aws.s3.bucket.profiles}")
  private String profilesBucket;

  @Value("${aws.s3.bucket.posts}")
  private String postsBucket;

  @Value("${aws.s3.bucket.products}")
  private String productsBucket;

  @Value("${aws.s3.bucket.default}")
  private String defaultBucket;

  // 허용된 이미지 확장자
  private final List<String> ALLOWED_EXTENSIONS =
      Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "heic", "svg");

  // 최대 파일 크기 (10MB)
  private final long MAX_FILE_SIZE = 10 * 1024 * 1024;

  /** 이미지 요청 경로에 따라 적절한 버킷을 선택합니다. */
  private String determineBucket(String requestPath) {
    // 요청 URL에서 패턴을 확인하여 이미지 용도를 파악
    if (requestPath != null) {
      if (Pattern.compile("/user|/profile", Pattern.CASE_INSENSITIVE).matcher(requestPath).find()) {
        return profilesBucket;
      } else if (Pattern.compile("/post", Pattern.CASE_INSENSITIVE).matcher(requestPath).find()) {
        return postsBucket;
      } else if (Pattern.compile("/product", Pattern.CASE_INSENSITIVE)
          .matcher(requestPath)
          .find()) {
        return productsBucket;
      }
    }

    // 기본값은 디폴트 버킷
    return defaultBucket;
  }

  /**
   * 단일 이미지 파일을 S3에 업로드합니다.
   *
   * @param file 업로드할 이미지 파일
   * @param requestPath 요청 경로
   * @return 업로드 결과 정보
   */
  public ImageResponseDTO uploadFile(MultipartFile file, String requestPath) {
    try {
      // 파일 유효성 검사
      validateFile(file);

      // 파일 정보 추출
      String originalFilename = file.getOriginalFilename();
      String extension = getFileExtension(originalFilename);
      String contentType = file.getContentType();
      String bucketName = determineBucket(requestPath);
      String uniqueFilename = generateUniqueFilename(extension);

      log.info("파일 업로드 버킷: {}, 파일명: {}", bucketName, uniqueFilename);

      // S3에 파일 업로드
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(uniqueFilename)
              .contentType(contentType)
              .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

      // 결과 DTO 생성
      return createSuccessResponse(file, uniqueFilename, contentType, bucketName);

    } catch (IllegalArgumentException e) {
      log.error("파일 유효성 검사 실패: {}", e.getMessage());
      return ImageResponseDTO.builder().error(e.getMessage()).build();
    } catch (Exception e) {
      log.error("파일 업로드 중 예외 발생: {}", e.getMessage(), e);
      return ImageResponseDTO.builder().error("파일 업로드 중 오류가 발생했습니다: " + e.getMessage()).build();
    }
  }

  /**
   * 다중 이미지 파일을 S3에 업로드합니다. (최대 3개)
   *
   * @param files 업로드할 이미지 파일 목록
   * @param requestPath 요청 경로
   * @return 업로드 결과 정보 목록
   */
  public List<ImageResponseDTO> uploadMultipleFiles(List<MultipartFile> files, String requestPath) {
    if (files == null || files.isEmpty()) {
      return new ArrayList<>();
    }

    if (files.size() > 3) {
      List<ImageResponseDTO> errorList = new ArrayList<>();
      errorList.add(ImageResponseDTO.builder().error("3개 이하의 파일을 업로드 하세요.").build());
      return errorList;
    }

    List<ImageResponseDTO> results = new ArrayList<>();
    for (MultipartFile file : files) {
      results.add(uploadFile(file, requestPath));
    }

    return results;
  }

  /**
   * 파일의 유효성을 검사합니다.
   *
   * @param file 검사할 파일
   * @throws IllegalArgumentException 유효하지 않은 파일인 경우
   */
  private void validateFile(MultipartFile file) {
    // 빈 파일 검사
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
    }

    // 파일 크기 검사
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("10MB 이상의 이미지는 업로드할 수 없습니다.");
    }

    // 파일 확장자 검사
    String extension = getFileExtension(file.getOriginalFilename());
    if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
      throw new IllegalArgumentException(
          "이미지 파일만 업로드가 가능합니다. (지원 형식: " + String.join(", ", ALLOWED_EXTENSIONS) + ")");
    }
  }

  /**
   * 파일 확장자를 추출합니다.
   *
   * @param filename 파일명
   * @return 확장자
   */
  private String getFileExtension(String filename) {
    if (filename == null || filename.isEmpty()) {
      return "";
    }

    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      return "";
    }

    return filename.substring(dotIndex + 1).toLowerCase();
  }

  /**
   * 고유한 파일명을 생성합니다.
   *
   * @param extension 파일 확장자
   * @return 고유한 파일명
   */
  private String generateUniqueFilename(String extension) {
    return System.currentTimeMillis()
        + "-"
        + UUID.randomUUID().toString().substring(0, 8)
        + "."
        + extension;
  }

  /**
   * 성공 응답을 생성합니다.
   *
   * @param file 업로드된 파일
   * @param key S3 객체 키
   * @param contentType 컨텐츠 타입
   * @param bucketName 버킷 이름
   * @return 응답 DTO
   */
  private ImageResponseDTO createSuccessResponse(
      MultipartFile file, String key, String contentType, String bucketName) {
    // 7일 동안 유효한 서명된 URL 생성
    String presignedUrl = generatePresignedUrl(key, bucketName);

    return ImageResponseDTO.builder()
        .fieldname("image")
        .originalname(file.getOriginalFilename())
        .encoding("7bit")
        .mimetype(contentType)
        .destination(bucketName + "/")
        .bucketName(bucketName)
        .filename(key)
        .path(bucketName + "/" + key)
        .publicUrl(presignedUrl)
        .imageSrc(presignedUrl)
        .size(file.getSize())
        .build();
  }

  /**
   * 객체에 대한 미리 서명된 URL을 생성합니다.
   *
   * @param key 객체 키
   * @param bucketName 버킷 이름
   * @return 미리 서명된 URL
   */
  public String generatePresignedUrl(String key, String bucketName) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofDays(7)) // 7일 유효
            .getObjectRequest(getObjectRequest)
            .build();

    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }
}
