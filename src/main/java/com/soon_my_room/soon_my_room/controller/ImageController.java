package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.ImageResponseDTO;
import com.soon_my_room.soon_my_room.service.S3StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image", description = "이미지 업로드 관련 API")
public class ImageController {

  private final S3StorageService storageService;

  @Operation(summary = "단일 이미지 업로드", description = "프로필 이미지, 게시글 이미지 등 단일 이미지를 업로드합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "업로드 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청(이미지 파일이 아니거나, 크기가 너무 큰 경우)")
      })
  @PostMapping(value = "/uploadfile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadFile(
      @RequestParam("image") MultipartFile file, HttpServletRequest request) {

    log.info(
        "단일 이미지 업로드 요청: {}, 크기: {}, 요청 경로: {}",
        file.getOriginalFilename(),
        file.getSize(),
        request.getRequestURI());

    ImageResponseDTO result = storageService.uploadFile(file, request.getHeader("Referer"));

    if (result.getError() != null && !result.getError().isEmpty()) {
      log.error("이미지 업로드 실패: {}", result.getError());
      return ResponseEntity.badRequest().body(Map.of("message", result.getError()));
    }

    log.info("이미지 업로드 성공: {}", result.getFilename());
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "다중 이미지 업로드", description = "게시글 등에 사용할 여러 이미지(최대 3개)를 업로드합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "업로드 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청(이미지 파일이 아니거나, 3개 초과, 크기가 너무 큰 경우)")
      })
  @PostMapping(value = "/uploadfiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadFiles(
      @RequestParam("image") List<MultipartFile> files, HttpServletRequest request) {

    log.info("다중 이미지 업로드 요청: {} 개 파일, 요청 경로: {}", files.size(), request.getRequestURI());

    if (files.size() > 3) {
      return ResponseEntity.badRequest().body(Map.of("message", "3개 이하의 파일을 업로드 하세요."));
    }

    List<ImageResponseDTO> results =
        storageService.uploadMultipleFiles(files, request.getHeader("Referer"));

    // 에러 체크
    boolean hasError =
        results.stream()
            .anyMatch(result -> result.getError() != null && !result.getError().isEmpty());

    if (hasError) {
      String errorMessage =
          results.stream()
              .filter(result -> result.getError() != null && !result.getError().isEmpty())
              .findFirst()
              .map(ImageResponseDTO::getError)
              .orElse("파일 업로드 중 오류가 발생했습니다.");

      return ResponseEntity.badRequest().body(Map.of("message", errorMessage));
    }

    log.info("다중 이미지 업로드 성공: {} 개 파일", results.size());
    return ResponseEntity.ok(results);
  }
}
