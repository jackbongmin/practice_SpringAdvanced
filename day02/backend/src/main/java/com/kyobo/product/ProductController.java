package com.kyobo.product;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kyobo.product.ProductDtos.ChangeNameRequest;
import com.kyobo.product.ProductDtos.ProductRequest;
import com.kyobo.product.ProductDtos.ProductResponse;

/**
 * 1일차 (10_01) · 가져옴 → 2일차 · 02에서 PUT /product/price 추가
 *
 * [역할] 상품 API의 입구. 주소와 요청 값을 받아 ProductService에 넘기고, 결과를 JSON으로 돌려줌
 *   - GET /product?number=1 · POST /product · PUT /product · DELETE /product?number=1 (교재 7장과 같은 주소)
 *   - PUT /product/price?number=1&percent=10 (2일차 · 가이드 4-4 할 일 ⑤)
 *
 * [알아야 할 것]
 * - 계산 · 저장은 하지 않음. 서비스에 넘기기만 함 → 컨트롤러 테스트는 서비스를 @MockBean으로 바꿔 끼움
 * - 서비스가 IllegalArgumentException을 던지면 지금은 500. 400으로 바꾸는 표준 오류 응답은 10_06
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
}
