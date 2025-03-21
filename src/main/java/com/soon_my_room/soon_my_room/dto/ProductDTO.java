package com.soon_my_room.soon_my_room.dto;

import com.soon_my_room.soon_my_room.model.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductRequest {
    @Valid private ProductContent product;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductContent {
      @NotBlank(message = "상품명을 입력해주세요.")
      private String itemName;

      @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
      private int price;

      @NotBlank(message = "링크를 입력해주세요.")
      private String link;

      @NotBlank(message = "상품 이미지를 업로드해주세요.")
      private String itemImage;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductResponse {
    private ProductDetail product;

    public static ProductResponse fromEntity(Product product, ProfileDTO.Profile author) {
      ProductDetail productDetail =
          ProductDetail.builder()
              .id(product.getId())
              .itemName(product.getItemName())
              .price(product.getPrice())
              .link(product.getLink())
              .itemImage(product.getItemImage())
              .author(author)
              .build();

      return ProductResponse.builder().product(productDetail).build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductListResponse {
    private int data;
    private List<ProductDetail> product;

    public static ProductListResponse fromEntities(List<ProductDetail> productDetails) {
      return ProductListResponse.builder()
          .data(productDetails.size())
          .product(productDetails)
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductDetail {
    private String id;
    private String itemName;
    private int price;
    private String link;
    private String itemImage;
    private ProfileDTO.Profile author;
  }
}
