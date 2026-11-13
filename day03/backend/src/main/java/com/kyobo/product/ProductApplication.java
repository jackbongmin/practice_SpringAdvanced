package com.kyobo.product;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * 1일차 (10_01) · 가져옴 → 3일차 · 03에서 @EnableSpringDataWebSupport 추가
 *
 * [역할] 스프링 부트 시작점. main을 실행하면 내장 톰캣(8080)과 H2 메모리 DB가 뜸
 *
 * [알아야 할 것]
 * - @SpringBootApplication이 있는 패키지(com.kyobo.product)와 그 아래만 빈으로 찾음
 * - H2 메모리 DB라서 서버를 끄면 데이터가 사라짐. 3일차부터는 서버가 뜰 때 DataInitializer가 상품 8개를 넣어 둠
 * - 가이드 03 4-5 ②: Page를 JSON으로 그대로 내보내면 경고가 찍힘 → @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) 한 줄을 더함
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) // 4-5 ②: Page를 {content, page} 모양으로 내보냄. 빠뜨리면 PageImpl을 그대로 내보내며 "Serializing PageImpl instances as-is" 경고
public class ProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}
}
