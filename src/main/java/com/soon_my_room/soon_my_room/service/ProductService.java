package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.ProductDTO;
import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.Follow;
import com.soon_my_room.soon_my_room.model.Product;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.ProductRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final FollowRepository followRepository;

  /** 상품 등록 */
  @Transactional
  public ProductDTO.ProductResponse createProduct(
      String userEmail, ProductDTO.ProductRequest.ProductContent productContent) {
    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 상품 생성
    Product product =
        Product.builder()
            .itemName(productContent.getItemName())
            .price(productContent.getPrice())
            .link(productContent.getLink())
            .itemImage(productContent.getItemImage())
            .author(currentUser)
            .build();

    Product savedProduct = productRepository.save(product);

    // 프로필 정보 구성
    List<String> followerIds =
        followRepository.findByFollowingId(currentUser.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(currentUser.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            currentUser,
            false, // 자신의 상품이므로 팔로우 상태는 false
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    // 응답 생성
    return ProductDTO.ProductResponse.fromEntity(savedProduct, authorProfile);
  }

  /** 사용자별 상품 목록 조회 */
  @Transactional(readOnly = true)
  public ProductDTO.ProductListResponse getUserProducts(
      String accountname, String currentUserEmail, Integer limit, Integer skip) {
    // 계정 소유자 조회
    User targetUser =
        userRepository
            .findByAccountname(accountname)
            .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 페이징 설정
    int pageSize = limit != null ? limit : 10;
    int pageNumber = skip != null ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    // 사용자 상품 조회
    List<Product> userProducts =
        productRepository.findByAuthorOrderByCreatedAtDesc(targetUser, pageable);

    // 상품이 없는 경우 빈 목록 반환
    if (userProducts.isEmpty()) {
      return ProductDTO.ProductListResponse.builder().data(0).product(new ArrayList<>()).build();
    }

    // 프로필 정보 구성
    List<String> followerIds =
        followRepository.findByFollowingId(targetUser.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(targetUser.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    boolean isFollowing =
        followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            targetUser,
            isFollowing,
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    // 상품 상세 정보 구성
    List<ProductDTO.ProductDetail> productDetails =
        userProducts.stream()
            .map(
                product ->
                    ProductDTO.ProductDetail.builder()
                        .id(product.getId())
                        .itemName(product.getItemName())
                        .price(product.getPrice())
                        .link(product.getLink())
                        .itemImage(product.getItemImage())
                        .author(authorProfile)
                        .build())
            .collect(Collectors.toList());

    return ProductDTO.ProductListResponse.fromEntities(productDetails);
  }

  /** 상품 상세 조회 */
  @Transactional(readOnly = true)
  public ProductDTO.ProductResponse getProductDetail(String productId, String currentUserEmail) {
    // 상품 조회
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("등록된 상품이 없습니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 작성자 프로필 정보
    User author = product.getAuthor();
    List<String> followerIds =
        followRepository.findByFollowingId(author.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(author.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    boolean isFollowing =
        followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), author.getId());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            author,
            isFollowing,
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    return ProductDTO.ProductResponse.fromEntity(product, authorProfile);
  }

  /** 상품 수정 */
  @Transactional
  public ProductDTO.ProductResponse updateProduct(
      String productId,
      String currentUserEmail,
      ProductDTO.ProductRequest.ProductContent productContent) {
    // 상품 조회
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("등록된 상품이 없습니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 상품 작성자 확인
    if (!product.getAuthor().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("잘못된 요청입니다. 로그인 정보를 확인하세요.");
    }

    // 상품 수정
    product.setItemName(productContent.getItemName());
    product.setPrice(productContent.getPrice());
    product.setLink(productContent.getLink());
    product.setItemImage(productContent.getItemImage());

    Product updatedProduct = productRepository.save(product);

    // 작성자 프로필 정보
    User author = updatedProduct.getAuthor();
    List<String> followerIds =
        followRepository.findByFollowingId(author.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(author.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            author,
            false, // 본인 상품
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    return ProductDTO.ProductResponse.fromEntity(updatedProduct, authorProfile);
  }

  /** 상품 삭제 */
  @Transactional
  public void deleteProduct(String productId, String currentUserEmail) {
    // 상품 조회
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("등록된 상품이 없습니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 상품 작성자 확인
    if (!product.getAuthor().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("잘못된 요청입니다. 로그인 정보를 확인하세요.");
    }

    // 상품 삭제
    productRepository.delete(product);
  }
}
