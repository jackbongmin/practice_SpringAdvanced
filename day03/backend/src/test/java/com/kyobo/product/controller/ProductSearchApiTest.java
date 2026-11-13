package com.kyobo.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import com.kyobo.product.dto.ProductDtos.ProductResponse;
import com.kyobo.product.service.ProductService;

/**
 * 3일차 · 03 (쿼리 메서드 · 정렬 · 페이지 · JPQL)
 *
 * [역할] GET /product/search의 주소 값(page · size · sort)이 Pageable로 바뀌는지, 응답이 {content, page} 모양인지 확인함
 *
 * [알아야 할 것]
 * - @WebMvcTest(ProductController.class): 컨트롤러만 띄움. 서비스는 @MockBean 가짜라 DB는 쓰지 않음
 * - ArgumentCaptor로 컨트롤러가 서비스에 실제로 넘긴 Pageable을 붙잡아 페이지 번호 · 크기 · 정렬을 확인함
 * - 응답 JSON의 $.page.totalElements 같은 경로는 ProductApplication의 VIA_DTO 설정이 있어야 생김 (가이드 03 4-5 ②)
 * - 끝나면 테스트 2개
 */
@WebMvcTest(ProductController.class) // 컨트롤러만 띄움 (DB · 서비스 진짜 없음)
class ProductSearchApiTest {

	@Autowired
	MockMvc mvc; // 서버를 켜지 않고 HTTP 요청을 흉내 내는 도구

	@MockBean // 진짜 서비스 대신 가짜를 스프링에 넣음
	ProductService service;

	@Test
	void 검색_파라미터가_Pageable로_바뀐다() throws Exception {
		given(service.search(eq("펜"), any(Pageable.class))).willReturn(new PageImpl<>( // 가짜 서비스의 대답: 전체 5개 중 1페이지(크기 2)
				List.of(new ProductResponse(5L, "형광펜", 1200, 12)), PageRequest.of(1, 2), 5));
		// eq("펜"): 인수 하나에 매처(any)를 쓰면 나머지도 매처여야 함. "펜"을 그대로 쓰면 InvalidUseOfMatchersException

		mvc.perform(get("/product/search").param("keyword", "펜")
				.param("page", "1").param("size", "2").param("sort", "price,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("형광펜"))
				.andExpect(jsonPath("$.page.totalElements").value(5)) // $.page.… 는 VIA_DTO 모양일 때만 있음
				.andExpect(jsonPath("$.page.totalPages").value(3))
				.andExpect(jsonPath("$.page.number").value(1));

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class); // 컨트롤러가 서비스에 실제로 넘긴 Pageable을 붙잡을 그릇
		verify(service).search(eq("펜"), captor.capture());
		assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
		assertThat(captor.getValue().getPageSize()).isEqualTo(2);
		assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("price"))); // "price,desc" → 가격 내림차순
	}

	@Test
	void 아무것도_안_주면_기본값_size5_번호순() throws Exception {
		given(service.search(eq("펜"), any(Pageable.class))).willReturn(new PageImpl<>(List.of())); // 빈 결과

		mvc.perform(get("/product/search").param("keyword", "펜")).andExpect(status().isOk());

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(service).search(eq("펜"), captor.capture());
		assertThat(captor.getValue().getPageNumber()).isZero();                 // page 기본값 0
		assertThat(captor.getValue().getPageSize()).isEqualTo(5);               // @PageableDefault(size = 5)
		assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("number"));   // @PageableDefault(sort = "number")
	}
}
