package com.soon_my_room.soon_my_room.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.soon_my_room.soon_my_room.dto.ImageResponseDTO;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock private S3Client s3Client;

    @Mock private S3Presigner s3Presigner;

    @InjectMocks private S3StorageService s3StorageService;

    @Mock private PresignedGetObjectRequest presignedGetObjectRequest;

    // 테스트 변수
    private MockMultipartFile validImageFile;
    private MockMultipartFile largeImageFile;
    private MockMultipartFile invalidExtensionFile;
    private String testProfilesBucket = "test-profiles";
    private String testPostsBucket = "test-posts";
    private String testProductsBucket = "test-products";
    private String testDefaultBucket = "test-default";

    @BeforeEach
    void setUp() throws IOException {
        // 유효한 이미지 파일 (10KB)
        byte[] validImageContent = new byte[10 * 1024];
        Arrays.fill(validImageContent, (byte) 1);
        validImageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                validImageContent
        );

        // 크기가 큰 이미지 파일 (11MB)
        byte[] largeImageContent = new byte[11 * 1024 * 1024];
        Arrays.fill(largeImageContent, (byte) 1);
        largeImageFile = new MockMultipartFile(
                "image",
                "large-image.jpg",
                "image/jpeg",
                largeImageContent
        );

        // 잘못된 확장자 파일
        byte[] invalidExtensionContent = new byte[1024];
        Arrays.fill(invalidExtensionContent, (byte) 1);
        invalidExtensionFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                invalidExtensionContent
        );

        // 서비스에 버킷 이름 설정
        ReflectionTestUtils.setField(s3StorageService, "profilesBucket", testProfilesBucket);
        ReflectionTestUtils.setField(s3StorageService, "postsBucket", testPostsBucket);
        ReflectionTestUtils.setField(s3StorageService, "productsBucket", testProductsBucket);
        ReflectionTestUtils.setField(s3StorageService, "defaultBucket", testDefaultBucket);
    }

    @Test
    @DisplayName("단일 이미지 업로드 성공 테스트")
    void uploadFile_Success() throws IOException {
        // Given
        String requestPath = "/api/profile/123";
        String expectedBucket = testProfilesBucket;
        String presignedUrl = "https://test-bucket.s3.amazonaws.com/image.jpg";

        // S3 Presigner 모킹
        URL url = new URL(presignedUrl);
        when(presignedGetObjectRequest.url()).thenReturn(url);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);

        // When
        ImageResponseDTO result = s3StorageService.uploadFile(validImageFile, requestPath);

        // Then
        assertNotNull(result);
        assertNull(result.getError());
        assertEquals(validImageFile.getOriginalFilename(), result.getOriginalname());
        assertEquals(validImageFile.getContentType(), result.getMimetype());
        assertEquals(validImageFile.getSize(), result.getSize());
        assertEquals(presignedUrl, result.getPublicUrl());
        assertEquals(presignedUrl, result.getImageSrc());
        assertEquals(expectedBucket, result.getBucketName());

        // Verify
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("단일 이미지 업로드 실패 - 파일 크기 초과")
    void uploadFile_FileSizeExceeded() {
        // Given
        String requestPath = "/api/post/123";

        // When
        ImageResponseDTO result = s3StorageService.uploadFile(largeImageFile, requestPath);

        // Then
        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("10MB 이상의 이미지는 업로드할 수 없습니다"));

        // Verify
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("단일 이미지 업로드 실패 - 잘못된 파일 형식")
    void uploadFile_InvalidFileType() {
        // Given
        String requestPath = "/api/product/123";

        // When
        ImageResponseDTO result = s3StorageService.uploadFile(invalidExtensionFile, requestPath);

        // Then
        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("이미지 파일만 업로드가 가능합니다"));

        // Verify
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공 테스트")
    void uploadMultipleFiles_Success() throws IOException {
        // Given
        String requestPath = "/api/post/123";
        String presignedUrl = "https://test-bucket.s3.amazonaws.com/image.jpg";
        List<MultipartFile> files = Arrays.asList(validImageFile, validImageFile);

        // S3 Presigner 모킹
        URL url = new URL(presignedUrl);
        when(presignedGetObjectRequest.url()).thenReturn(url);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);

        // When
        List<ImageResponseDTO> results = s3StorageService.uploadMultipleFiles(files, requestPath);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertNull(results.get(0).getError());
        assertNull(results.get(1).getError());

        // Verify
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("다중 이미지 업로드 실패 - 파일 수 초과")
    void uploadMultipleFiles_TooManyFiles() {
        // Given
        String requestPath = "/api/post/123";
        List<MultipartFile> files = Arrays.asList(
                validImageFile, validImageFile, validImageFile, validImageFile  // 4개 파일
        );

        // When
        List<ImageResponseDTO> results = s3StorageService.uploadMultipleFiles(files, requestPath);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).getError());
        assertTrue(results.get(0).getError().contains("3개 이하의 파일을 업로드 하세요"));

        // Verify
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("다중 이미지 업로드 - 빈 파일 리스트")
    void uploadMultipleFiles_EmptyList() {
        // Given
        String requestPath = "/api/post/123";
        List<MultipartFile> files = Arrays.asList();

        // When
        List<ImageResponseDTO> results = s3StorageService.uploadMultipleFiles(files, requestPath);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());

        // Verify
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("버킷 결정 로직 테스트 - 프로필 이미지")
    void determineBucket_ProfileImage() {
        // Given
        String profileRequestPath = "/api/profile/update";
        String userRequestPath = "/api/user/image";

        // When & Then - 리플렉션을 통해 private 메소드 테스트
        assertEquals(testProfilesBucket, ReflectionTestUtils.invokeMethod(
                s3StorageService, "determineBucket", profileRequestPath));
        assertEquals(testProfilesBucket, ReflectionTestUtils.invokeMethod(
                s3StorageService, "determineBucket", userRequestPath));
    }

    @Test
    @DisplayName("버킷 결정 로직 테스트 - 게시글 이미지")
    void determineBucket_PostImage() {
        // Given
        String postRequestPath = "/api/post/upload";

        // When & Then
        assertEquals(testPostsBucket, ReflectionTestUtils.invokeMethod(
                s3StorageService, "determineBucket", postRequestPath));
    }

    @Test
    @DisplayName("버킷 결정 로직 테스트 - 상품 이미지")
    void determineBucket_ProductImage() {
        // Given
        String productRequestPath = "/api/product/upload";

        // When & Then
        assertEquals(testProductsBucket, ReflectionTestUtils.invokeMethod(
                s3StorageService, "determineBucket", productRequestPath));
    }

    @Test
    @DisplayName("버킷 결정 로직 테스트 - 기본 버킷")
    void determineBucket_DefaultBucket() {
        // Given
        String unknownRequestPath = "/api/unknown/path";
        String nullRequestPath = null;

        // When & Then
        assertEquals(testDefaultBucket, ReflectionTestUtils.invokeMethod(
                s3StorageService, "determineBucket", unknownRequestPath));
        assertEquals(testDefaultBucket, ReflectionTestUtils.invokeMethod(
                s3StorageService, "determineBucket", nullRequestPath));
    }

    @Test
    @DisplayName("미리 서명된 URL 생성 테스트")
    void generatePresignedUrl_Success() throws IOException {
        // Given
        String key = "test-image.jpg";
        String bucket = testProfilesBucket;
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/test-image.jpg";

        // S3 Presigner 모킹
        URL url = new URL(expectedUrl);
        when(presignedGetObjectRequest.url()).thenReturn(url);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);

        // When
        String result = s3StorageService.generatePresignedUrl(key, bucket);

        // Then
        assertNotNull(result);
        assertEquals(expectedUrl, result);

        // Verify
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }
}