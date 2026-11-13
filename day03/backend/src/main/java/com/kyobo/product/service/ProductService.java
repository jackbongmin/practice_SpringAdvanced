package com.kyobo.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kyobo.product.dto.ProductDtos.ProductRequest;
import com.kyobo.product.dto.ProductDtos.ProductResponse;
import com.kyobo.product.entity.Product;
import com.kyobo.product.exception.ProductNotFoundException;
import com.kyobo.product.repository.ProductRepository;

/**
 * 1일차 (10_01) · 가져옴 → 2일차 · 02에서 raisePrice 추가 → 3일차 · 03에서 search 추가
 *
 * [역할] 상품 기능의 본체. 조회 · 저장 · 이름 변경 · 삭제, 가격 인상(PricePolicy에 계산을 맡김), 이름 검색 + 페이지
 *
 * [알아야 할 것]
 * - 저장소 · 가격 정책을 생성자로 받음 → 테스트에서 가짜 저장소와 진짜 PricePolicy를 넣을 수 있음
 * - 커버리지 규칙: 줄 커버리지 90% 미만이면 ./gradlew check 실패 (가이드 02 4-3, build.gradle)
 * - search는 엔티티 Page를 Page.map(ProductResponse::from)으로 응답 DTO Page로 바꿔 돌려줌 (가이드 03 4-5)
 */
@Service
public class ProductService {

	private final ProductRepository products;
	private final PricePolicy pricePolicy; // 4-4: 가격 계산은 PricePolicy에 맡긴다

	// 생성자로 받는다 → 테스트에서 가짜 저장소 · 진짜 PricePolicy를 넣을 수 있다
	public ProductService(ProductRepository products, PricePolicy pricePolicy) {
		this.products = products;
		this.pricePolicy = pricePolicy;
	}

	public ProductResponse getProduct(Long number) {
		return ProductResponse.from(find(number));
	}

	// 4-5: 이름 검색 + 페이지. 엔티티 Page를 응답 DTO Page로 바꿔 내보낸다 (엔티티를 그대로 밖에 내보내지 않음)
	public Page<ProductResponse> search(String keyword, Pageable pageable) {
		return products.findByNameContaining(keyword, pageable).map(ProductResponse::from); // Page.map: 내용만 바꾸고 페이지 정보(전체 개수 등)는 그대로
	}

	@Transactional
	public ProductResponse saveProduct(ProductRequest req) {
		Product saved = products.save(new Product(req.name(), req.price(), req.stock()));
		return ProductResponse.from(saved);
	}

	@Transactional
	public ProductResponse changeName(Long number, String name) {
		Product product = find(number);
		product.changeName(name);
		return ProductResponse.from(product);
	}

	// 4-4: 상품을 찾아 PricePolicy가 계산한 가격으로 바꾼다
	@Transactional
	public ProductResponse raisePrice(Long number, int percent) {
		Product product = find(number); // 없는 번호면 ProductNotFoundException → 404
		product.changePrice(pricePolicy.raisePrice(product.getPrice(), percent));
		return ProductResponse.from(product);
	}

	@Transactional
	public void deleteProduct(Long number) {
		products.delete(find(number));
	}

	// 3단계: 교재 원본은 findById(number).get() — 없는 번호면 NoSuchElementException(500)
	private Product find(Long number) {
		return products.findById(number).orElseThrow(() -> new ProductNotFoundException(number));
	}
}
