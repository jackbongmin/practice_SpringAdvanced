package com.kyobo.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kyobo.product.dto.ProductDtos.ChangeNameRequest;
import com.kyobo.product.dto.ProductDtos.ProductRequest;
import com.kyobo.product.dto.ProductDtos.ProductResponse;
import com.kyobo.product.service.ProductService;

/**
 * 1일차 (10_01) · 가져옴 → 2일차 · 02에서 PUT /product/price 추가 → 3일차 · 03에서 GET /product/search 추가
 *
 * [역할] 상품 API의 입구. 주소와 요청 값을 받아 ProductService에 넘기고, 결과를 JSON으로 돌려줌
 *   - GET /product?number=1 · POST /product · PUT /product · DELETE /product?number=1 (교재 7장과 같은 주소)
 *   - PUT /product/price?number=1&percent=10 (2일차 · 가이드 02 4-4 할 일 ⑤)
 *   - GET /product/search?keyword=펜&page=0&size=2&sort=price,desc (3일차 · 가이드 03 4-5)
 *
 * [알아야 할 것]
 * - 계산 · 저장은 하지 않음. 서비스에 넘기기만 함 → 컨트롤러 테스트는 서비스를 @MockBean으로 바꿔 끼움
 * - 주소의 page · size · sort는 스프링이 Pageable 하나로 묶어 줌. 아무것도 안 오면 @PageableDefault 값을 씀
 * - 서비스가 IllegalArgumentException을 던지거나 없는 칸으로 정렬하면 지금은 500. 400으로 바꾸는 표준 오류 응답은 10_06
 */
@RestController
@RequestMapping("/product")
public class ProductController {

	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	public ProductResponse get(@RequestParam Long number) {
		return service.getProduct(number);
	}

	// 4-5: GET /product/search?keyword=펜&page=1&size=2&sort=price,desc
	@GetMapping("/search")
	public Page<ProductResponse> search(@RequestParam String keyword, // keyword는 필수. 아예 빼면 400, keyword=(빈 값)는 전체 검색
			@PageableDefault(size = 5, sort = "number") Pageable pageable) { // 주소의 page · size · sort를 스프링이 Pageable로 묶어 줌. 안 오면 0페이지 · 5개 · 번호순
		return service.search(keyword, pageable);
	}

	@PostMapping
	public ProductResponse create(@RequestBody ProductRequest req) {
		return service.saveProduct(req);
	}

	@PutMapping
	public ProductResponse changeName(@RequestBody ChangeNameRequest req) {
		return service.changeName(req.number(), req.name());
	}

	@DeleteMapping
	public String delete(@RequestParam Long number) {
		service.deleteProduct(number);
		return "정상적으로 삭제되었습니다.";
	}

	// 4-4: PUT /product/price?number=1&percent=10
	@PutMapping("/price")
	public ProductResponse raisePrice(@RequestParam Long number, @RequestParam int percent) { // 주소의 ?number=...&percent=... 값을 받음
		return service.raisePrice(number, percent);
	}
}
