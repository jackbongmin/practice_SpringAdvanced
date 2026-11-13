package com.kyobo.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.kyobo.product.dto.ProductSummary;
import com.kyobo.product.entity.Product;

/**
 * 3일차 · 03 (쿼리 메서드 · 정렬 · 페이지 · JPQL)
 *
 * [역할] 같은 상품 8개를 넣어 두고 ProductRepository에 더한 조회 메서드의 결과를 하나씩 확인함
 *   - 4-0 이름으로_찾기 → 4-1 주제_키워드 → 4-2 조건_키워드 → 4-3 정렬 · 페이지는_0부터 → 4-4 JPQL · 프로젝션 · 벌크 수정
 *
 * [알아야 할 것]
 * - @DataJpaTest: JPA와 H2 메모리 DB만 띄움. 테스트마다 트랜잭션을 열고 끝나면 롤백 → 매번 setUp의 8개에서 시작
 * - names(...) · prices(...)는 상품 목록을 이름 · 가격 목록으로 바꾸는 도우미. 단언을 짧게 쓰려고 만듦
 * - 실행된 SQL은 테스트 보고서(build/reports/tests/test)의 Standard output에 보임 (application.properties의 show-sql)
 * - 끝나면 테스트 7개
 */
@DataJpaTest // JPA + H2만 띄움. 테스트마다 끝나면 롤백
class ProductQueryTest {

    @Autowired // 스프링이 만든 저장소 구현을 넣어 줌
	ProductRepository products;

	@BeforeEach // 테스트마다 먼저 실행 → 매번 같은 8개에서 시작
	void setUp() {
		products.saveAll(List.of(
				new Product("펜", 1000, 10),
				new Product("펜", 1500, 5),
				new Product("펜", 2000, 0),
				new Product("볼펜", 800, 30),
				new Product("형광펜", 1200, 12),
				new Product("공책", 3000, 7),
				new Product("연필", 500, 50),
				new Product("필통", 5000, 2)));
	}

	// 상품 목록 → 이름 목록 / 가격 목록 (단언을 짧게 쓰는 도우미)
	static List<String> names(List<Product> list) {
		return list.stream().map(Product::getName).toList();
	}

	static List<Integer> prices(List<Product> list) {
		return list.stream().map(Product::getPrice).toList();
	}

	@Test
	void 이름으로_찾기() {
		assertThat(products.findByName("펜")).hasSize(3); // 이름이 정확히 "펜"인 것만 (볼펜 · 형광펜 제외)
	}

    @Test
    void 주제_키워드() {
        assertThat(products.existsByName("지우개")).isFalse(); // 없는 이름 → false
        assertThat(products.countByName("펜")).isEqualTo(3);
        assertThat(products.findFirstByNameOrderByPriceAsc("펜")).get() // Optional 안의 값을 꺼내 검사
                .extracting(Product::getPrice).isEqualTo(1000);         // 펜 3개 중 가장 싼 것
        assertThat(prices(products.findTop3ByOrderByPriceDesc())).containsExactly(5000, 3000, 2000); // 순서까지 같아야 통과
    }

	// 4-2: 조건 키워드
	@Test
	void 조건_키워드() {
		assertThat(products.findByNameNot("펜")).hasSize(5); // 8개 중 펜 3개를 뺀 5개
		assertThat(prices(products.findByNameAndPriceLessThan("펜", 1500))).containsExactly(1000); // 1500은 "미만"이라 빠짐
		assertThat(prices(products.findByPriceBetween(1000, 2000))).containsExactlyInAnyOrder(1000, 1500, 2000, 1200); // 정렬이 없으니 순서는 상관없이 이 4개
		assertThat(names(products.findByNameContaining("펜"))).containsExactlyInAnyOrder("펜", "펜", "펜", "볼펜", "형광펜"); // like '%펜%' → 볼펜 · 형광펜도 포함
	}

	// 4-3: 정렬
	@Test
	void 정렬() {
		List<Product> sorted = products.findByNameContaining("펜",
				Sort.by(Sort.Order.desc("price"), Sort.Order.asc("name"))); // 가격 내림차순, 가격이 같으면 이름 오름차순. 칸 이름은 필드 이름
		assertThat(prices(sorted)).containsExactly(2000, 1500, 1200, 1000, 800); // 정렬했으니 순서까지 확인
	}

	// 4-3: 페이지
	@Test
	void 페이지는_0부터() {
		Page<Product> page = products.findByNameContaining("펜", PageRequest.of(1, 2, Sort.by("price"))); // (페이지 번호, 크기, 정렬). 번호는 0부터 → 1은 두 번째 페이지

		// 가격순 800, 1000 | 1200, 1500 | 2000 → 두 번째 페이지(번호 1)
		assertThat(prices(page.getContent())).containsExactly(1200, 1500); // 이 페이지의 내용
		assertThat(page.getTotalElements()).isEqualTo(5);                  // 조건에 맞는 전체 개수 (count 쿼리로 셈)
		assertThat(page.getTotalPages()).isEqualTo(3);                     // 5개 ÷ 2개씩 = 3페이지
		assertThat(page.getNumber()).isEqualTo(1);                         // 지금 페이지 번호 (0부터)
		assertThat(page.hasNext()).isTrue();                               // 3번째 페이지가 남아 있음
	}

	// 4-4 ①: JPQL
	@Test
	void JPQL() {
		assertThat(prices(products.findExpensive(2000))).containsExactly(5000, 3000, 2000); // 2000 "이상"이라 2000 포함, 쿼리의 order by로 내림차순
	}

	// 4-4 ②: 프로젝션
	@Test
	void 프로젝션은_필요한_칸만() {
		List<ProductSummary> rows = products.findSummaryByNameContaining("형광");

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getName()).isEqualTo("형광펜");
		assertThat(rows.get(0).getPrice()).isEqualTo(1200); // 번호 · 재고는 아예 읽지 않음 (SQL: select p1_0.name,p1_0.price ...)
	}

	// 4-4 ③ · ④: 벌크 수정
	@Test
	void 벌크_수정_후에는_다시_읽어야_새_값() {
		Product pen = products.findFirstByNameOrderByPriceAsc("펜").orElseThrow(); // 1000원 펜을 먼저 읽어 둠 → 영속성 컨텍스트(책상 위)에 올라감

		int updated = products.raiseAll("펜", 100); // DB(창고)에서 펜 3개를 한 번에 +100. 책상 위의 옛 객체는 그대로

		assertThat(updated).isEqualTo(3); // 바뀐 행 수
		// clearAutomatically = true라서 책상을 비웠음 → findById가 DB에서 새로 읽어 1100. 없으면 옛 객체(1000)를 돌려줘 실패
		assertThat(products.findById(pen.getNumber())).get().extracting(Product::getPrice).isEqualTo(1100);
	}
}
