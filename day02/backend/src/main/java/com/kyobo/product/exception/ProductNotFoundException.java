package com.kyobo.product.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 1일차 (10_01) · 가져옴
 *
 * [역할] 없는 상품 번호일 때 던지는 예외. @ResponseStatus 덕분에 응답이 500이 아니라 404
 *
 * [알아야 할 것]
 * - 메시지에 번호를 넣어 둠("상품 없음: 999") → 테스트에서 hasMessageContaining("999")로 확인
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException(Long number) {
		super("상품 없음: " + number);
	}
}
