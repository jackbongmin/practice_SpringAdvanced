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
class ProductSearchApiTest {

}
