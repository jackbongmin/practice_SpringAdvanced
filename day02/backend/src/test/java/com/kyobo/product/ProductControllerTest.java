package com.kyobo.product;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.kyobo.product.ProductDtos.ProductRequest;
import com.kyobo.product.ProductDtos.ProductResponse;

/**
 * 1일차 (10_01) · 가져옴
 *
 * [역할] 컨트롤러만 띄워(@WebMvcTest) 주소 · JSON · 상태 코드를 확인함. 서비스는 가짜 빈(@MockBean)
 *
 * [알아야 할 것]
 * - 오늘은 고치지 않음. 서비스는 가짜라서 이 테스트는 ProductService의 커버리지를 올리지 않음
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	MockMvc mvc;

	@MockBean
	ProductService service;

	@Test
	void GET_product_번호로_조회() throws Exception {
		given(service.getProduct(123L)).willReturn(new ProductResponse(123L, "pen", 5000, 2000));

		mvc.perform(get("/product").param("number", "123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.number").value(123))
				.andExpect(jsonPath("$.name").value("pen"))
				.andExpect(jsonPath("$.price").value(5000))
				.andExpect(jsonPath("$.stock").value(2000));
		verify(service).getProduct(123L);
	}

	@Test
	void POST_product_JSON_본문이_요청_객체로_바뀐다() throws Exception {
		given(service.saveProduct(new ProductRequest("pen", 5000, 2000)))
				.willReturn(new ProductResponse(1L, "pen", 5000, 2000));

		mvc.perform(post("/product")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"pen\",\"price\":5000,\"stock\":2000}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.number").value(1));
		// record는 값이 같으면 equals → 같은 요청 객체로 불렸는지 확인할 수 있다
		verify(service).saveProduct(new ProductRequest("pen", 5000, 2000));
	}

	@Test
	void 없는_상품은_404() throws Exception {
		given(service.getProduct(999L)).willThrow(new ProductNotFoundException(999L));

		mvc.perform(get("/product").param("number", "999"))
				.andExpect(status().isNotFound());
	}
}
