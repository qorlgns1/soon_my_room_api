package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.ProductDTO;
import com.soon_my_room.soon_my_room.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Tag(name = "Product", description = "상품 관련 API")
public class ProductController {

  private final ProductService productService;

  @Operation(
      summary = "상품 등록",
      description = "새로운 상품을 등록합니다. 상품명, 가격, 링크, 이미지는 필수 입력사항입니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "상품 등록 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 또는 가격을 숫자로 입력하지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
      })
  @PostMapping
  public ResponseEntity<?> createProduct(
      @Parameter(description = "상품 정보", required = true) @Valid @RequestBody
          ProductDTO.ProductRequest requestDTO,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      ProductDTO.ProductResponse response =
          productService.createProduct(currentUserEmail, requestDTO.getProduct());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "상품 목록 조회",
      description = "특정 사용자의 상품 목록을 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "계정이 존재하지 않음")
      })
  @GetMapping("/{accountname}")
  public ResponseEntity<?> getUserProducts(
      @Parameter(description = "계정명", required = true) @PathVariable String accountname,
      @Parameter(description = "페이지당 상품 수") @RequestParam(required = false) Integer limit,
      @Parameter(description = "건너뛸 상품 수") @RequestParam(required = false) Integer skip,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      ProductDTO.ProductListResponse response =
          productService.getUserProducts(accountname, currentUserEmail, limit, skip);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("message", "해당 계정이 존재하지 않습니다."));
    }
  }

  @Operation(
      summary = "상품 상세 조회",
      description = "특정 상품의 상세 정보를 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "상품이 존재하지 않음")
      })
  @GetMapping("/detail/{product_id}")
  public ResponseEntity<?> getProductDetail(
      @Parameter(description = "상품 ID", required = true) @PathVariable("product_id")
          String productId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      ProductDTO.ProductResponse response =
          productService.getProductDetail(productId, currentUserEmail);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "등록된 상품이 없습니다."));
    }
  }

  @Operation(
      summary = "상품 수정",
      description = "특정 상품의 정보를 수정합니다. 상품명, 가격, 링크, 이미지는 필수 입력사항입니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 또는 가격을 숫자로 입력하지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
        @ApiResponse(responseCode = "404", description = "상품이 존재하지 않음")
      })
  @PutMapping("/{product_id}")
  public ResponseEntity<?> updateProduct(
      @Parameter(description = "상품 ID", required = true) @PathVariable("product_id")
          String productId,
      @Parameter(description = "상품 정보", required = true) @Valid @RequestBody
          ProductDTO.ProductRequest requestDTO,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      ProductDTO.ProductResponse response =
          productService.updateProduct(productId, currentUserEmail, requestDTO.getProduct());
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "등록된 상품이 없습니다."));
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("message", "잘못된 요청입니다. 로그인 정보를 확인하세요."));
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("price")) {
        return ResponseEntity.badRequest().body(Map.of("message", "가격은 숫자로 입력하셔야 합니다."));
      }
      return ResponseEntity.badRequest().body(Map.of("message", "필수 입력사항을 입력해주세요."));
    }
  }

  @Operation(
      summary = "상품 삭제",
      description = "특정 상품을 삭제합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        @ApiResponse(responseCode = "404", description = "상품이 존재하지 않음")
      })
  @DeleteMapping("/{product_id}")
  public ResponseEntity<?> deleteProduct(
      @Parameter(description = "상품 ID", required = true) @PathVariable("product_id")
          String productId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      productService.deleteProduct(productId, currentUserEmail);
      return ResponseEntity.ok("삭제되었습니다.");
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "등록된 상품이 없습니다."));
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("message", "잘못된 요청입니다. 로그인 정보를 확인하세요."));
    }
  }
}
