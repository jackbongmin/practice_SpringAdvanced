package com.kyobo.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.kyobo.product.dto.ProductDtos.ProductRequest;
import com.kyobo.product.dto.ProductDtos.ProductResponse;
import com.kyobo.product.entity.Product;
import com.kyobo.product.exception.ProductNotFoundException;
import com.kyobo.product.repository.ProductRepository;

/**
 * 1일차 (10_01) · 가져옴 → 2일차 · 02에서 테스트 3개 추가 + setUp 변경
 *
 * [역할] 스프링 없이 ProductService만 테스트함. 저장소는 가짜(mock), 가격 정책은 진짜(new PricePolicy())
 *
 * [알아야 할 것]
 * - saved(...)는 "DB가 번호를 붙인 상품"을 흉내 내는 도우미. Product에 number setter가 없어서 ReflectionTestUtils로 넣음
 * - 가이드 4-2: 단언 없는 테스트로 커버리지만 올려 봄 → 단언 있는 테스트로 바꿈
 * - 가이드 4-3: 있는 상품 삭제 테스트를 더해 커버리지 규칙(90%)을 통과시킴
 * - 가이드 4-4 ⑤: setUp()을 new ProductService(products, new PricePolicy())로 바꾸고 가격_인상 테스트를 더함
 */
class ProductServiceTest {

	ProductRepository products = mock(ProductRepository.class);
	ProductService service;

	@BeforeEach
	void setUp() {
		service = new ProductService(products, new PricePolicy()); // PricePolicy는 순수 계산이라 mock 말고 진짜를 넣음 (4-5 참고)
	}

	// DB가 번호를 붙인 상품처럼 만든다 (number는 setter가 없다)
	static Product saved(Long number, String name, int price, int stock) {
		Product p = new Product(name, price, stock);
		ReflectionTestUtils.setField(p, "number", number);
		return p;
	}

	@Test
	void 번호로_조회하면_저장소의_상품을_응답으로_바꾼다() {
		// given
		given(products.findById(123L)).willReturn(Optional.of(saved(123L, "펜", 1000, 1234)));
		// when
		ProductResponse res = service.getProduct(123L);
		// then
		assertThat(res).isEqualTo(new ProductResponse(123L, "펜", 1000, 1234));
		verify(products).findById(123L);
	}

	@Test
	void 저장할_때_요청_값이_그대로_저장소에_넘어간다() {
		given(products.save(any(Product.class))).willReturn(saved(1L, "펜", 1000, 1234));

		service.saveProduct(new ProductRequest("펜", 1000, 1234));

		// 저장소에 실제로 넘어간 객체를 붙잡아 값을 확인한다
		ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
		verify(products).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("펜");
		assertThat(captor.getValue().getPrice()).isEqualTo(1000);
		assertThat(captor.getValue().getStock()).isEqualTo(1234);
	}

	@Test
	void 없는_번호를_조회하면_ProductNotFoundException() {
		given(products.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.getProduct(999L))
				.isInstanceOf(ProductNotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	void 없는_번호는_삭제하지_않는다() {
		given(products.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteProduct(999L)).isInstanceOf(ProductNotFoundException.class);
		verify(products, never()).delete(any());
	}

	// 4-2: 커버리지에 빨간 줄로 남았던 이름 변경 — 값까지 확인한다
	@Test
	void 이름을_바꾸면_응답에_새_이름() {
		given(products.findById(1L)).willReturn(Optional.of(saved(1L, "펜", 1000, 10)));

		ProductResponse res = service.changeName(1L, "볼펜");

		assertThat(res.name()).isEqualTo("볼펜"); // 단언: 응답의 이름이 정말 바뀌었나
	}

	// 4-3: 있는 상품 삭제 — 남은 빨간 줄(products.delete)을 채운다
	@Test
	void 있는_상품은_삭제한다() {
		Product pen = saved(1L, "펜", 1000, 10);
		given(products.findById(1L)).willReturn(Optional.of(pen));

		service.deleteProduct(1L);

		verify(products).delete(pen); // 단언: 저장소의 delete가 "그 펜"으로 불렸나
	}

	// 4-4: 서비스는 PricePolicy의 계산 결과를 상품에 반영한다
	@Test
	void 가격_인상() {
		given(products.findById(1L)).willReturn(Optional.of(saved(1L, "펜", 1650, 10)));

		assertThat(service.raisePrice(1L, 15).price()).isEqualTo(1898);
	}
}
