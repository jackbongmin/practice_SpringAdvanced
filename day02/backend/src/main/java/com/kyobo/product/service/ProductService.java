package com.kyobo.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kyobo.product.dto.ProductDtos.ProductRequest;
import com.kyobo.product.dto.ProductDtos.ProductResponse;
import com.kyobo.product.entity.Product;
import com.kyobo.product.exception.ProductNotFoundException;
import com.kyobo.product.repository.ProductRepository;

/**
 * 1일차 (10_01) · 가져옴 → 2일차 · 02에서 raisePrice 추가
 *
 * [역할] 상품 기능의 본체. 조회 · 저장 · 이름 변경 · 삭제, 그리고 가격 인상(PricePolicy에 계산을 맡김)
 *
 * [알아야 할 것]
 * - 오늘 커버리지(JaCoCo)를 재는 주인공. 보고서에서 changeName 본문 · deleteProduct의 delete 줄이 빨강 (가이드 4-1)
 * - 저장소 · 가격 정책을 생성자로 받음 → 테스트에서 가짜 저장소와 진짜 PricePolicy를 넣을 수 있음
 * - 4-4 할 일 ⑤에서 생성자에 PricePolicy를 더하면 ProductServiceTest.setUp()도 같이 고쳐야 컴파일됨
 * - 커버리지 규칙: 줄 커버리지 90% 미만이면 ./gradlew check 실패 (가이드 4-3)
 */
@Service
public class ProductService {

	private final ProductRepository products;

	// 생성자로 받는다 → 테스트에서 가짜(mock) 저장소를 넣을 수 있다
	public ProductService(ProductRepository products) {
		this.products = products;
	}

	public ProductResponse getProduct(Long number) {
		return ProductResponse.from(find(number));
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

	@Transactional
	public void deleteProduct(Long number) {
		products.delete(find(number));
	}

	// 3단계: 교재 원본은 findById(number).get() — 없는 번호면 NoSuchElementException(500)
	private Product find(Long number) {
		return products.findById(number).orElseThrow(() -> new ProductNotFoundException(number));
	}
}
