package com.kyobo.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 1일차 (10_01) · 가져옴
 *
 * [역할] 스프링 부트 시작점. main을 실행하면 내장 톰캣(8080)과 H2 메모리 DB가 뜸
 *
 * [알아야 할 것]
 * - @SpringBootApplication이 있는 패키지(com.kyobo.product)와 그 아래만 빈으로 찾음
 * - H2 메모리 DB라서 서버를 끄면 데이터가 사라짐 → curl로 확인할 때는 상품부터 등록
 */
@SpringBootApplication
public class ProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}
}
