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
class ProductQueryTest {

}
