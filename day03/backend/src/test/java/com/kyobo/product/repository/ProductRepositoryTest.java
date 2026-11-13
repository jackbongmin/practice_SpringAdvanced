package com.kyobo.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.kyobo.product.entity.Product;

/**
 * 1일차 (10_01) · 가져옴
 *
 * [역할] JPA와 메모리 DB(H2)만 띄워(@DataJpaTest) 저장 · 조회가 되는지 확인함
 *
 * [알아야 할 것]
 * - 테스트마다 끝나면 롤백 → 각 테스트는 빈 DB에서 시작함
 * - 오늘은 고치지 않음
 */
@DataJpaTest
class ProductRepositoryTest {

	@Autowired
	ProductRepository products;

	@Test
	void 저장하면_번호가_붙고_다시_찾을_수_있다() {
		Product saved = products.save(new Product("펜", 1000, 1234));

		assertThat(saved.getNumber()).isNotNull();
		assertThat(products.findById(saved.getNumber())).get()
				.extracting(Product::getName).isEqualTo("펜");
	}

	@Test
	void 이름으로_찾기() {
		products.save(new Product("펜", 1000, 10));
		products.save(new Product("펜", 2000, 20));
		products.save(new Product("공책", 3000, 30));

		assertThat(products.findByName("펜")).hasSize(2);
	}

	@Test
	void 각_테스트는_빈_DB에서_시작한다() {
		assertThat(products.count()).isZero();
	}


}
