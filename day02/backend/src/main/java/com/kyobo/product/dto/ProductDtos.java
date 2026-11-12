package com.kyobo.product.dto;

import com.kyobo.product.entity.Product;

/**
 * 1일차 (10_01) · 가져옴
 *
 * [역할] 요청 · 응답 모양(record) 모음. 교재의 ProductDto · ProductResponseDto · ChangeProductNameDto와 같은 칸
 *
 * [알아야 할 것]
 * - record는 값이 같으면 equals가 true → 테스트에서 new ProductResponse(...)와 바로 비교할 수 있음
 * - 엔티티(Product)를 그대로 내보내지 않고 ProductResponse.from(product)로 바꿔서 돌려줌
 * - from은 service 패키지에서 부르므로 public (패키지가 다르면 public이 아닌 메서드는 못 부름)
 * - AI 초안에 나오는 ProductResponseDto는 교재 이름. 우리 프로젝트에는 없음 (가이드 4-5)
 */
public final class ProductDtos {

	private ProductDtos() {
	}

	public record ProductRequest(String name, int price, int stock) {
	}

	public record ProductResponse(Long number, String name, int price, int stock) {

		public static ProductResponse from(Product p) {
			return new ProductResponse(p.getNumber(), p.getName(), p.getPrice(), p.getStock());
		}
	}

	public record ChangeNameRequest(Long number, String name) {
	}
}
