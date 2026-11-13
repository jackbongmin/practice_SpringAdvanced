package com.kyobo.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kyobo.product.dto.ProductSummary;
import com.kyobo.product.entity.Product;

/**
 * 1일차 (10_01) · 가져옴 → 3일차 · 03에서 쿼리 메서드 · 정렬 · 페이지 · JPQL · 프로젝션 · 벌크 수정 추가
 *
 * [역할] 상품 테이블을 읽고 쓰는 저장소. JpaRepository가 save · findById · delete 등을 만들어 줌
 *
 * [알아야 할 것]
 * - 인터페이스에 메서드 이름만 적으면 스프링 데이터 JPA가 이름을 읽어 쿼리를 만듦 (findByName → where name = ?)
 * - 이름 안의 칸 이름(Name, Price)은 테이블 컬럼이 아니라 Product의 필드 이름. 철자가 틀리면 서버 · 테스트 시작 자체가 실패함
 * - 결과가 여러 개일 수 있으면 List, 많아야 하나면 Optional로 받음 (가이드 03 4-2)
 * - 서비스 단위 테스트에서는 mock(ProductRepository.class)로 가짜를 넣음. 쿼리 자체는 ProductQueryTest(@DataJpaTest)로 확인함
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findByName(String name);

	Optional<Product> findFirstByNameOrderByPriceAsc(String name);	// 이름이 같은 것 중 가격이 가장 싼 1개. 없으면 Optional.empty()

	boolean existsByName(String name);	// 있나 없나만

	long countByName(String name);		// 몇개인가

	List<Product> findTop3ByOrderByPriceDesc();	// 가격 높은 순 3개

	// 4-2: 조건 키워드 — By 뒤의 단어가 where 절이 된다. 결과가 여러 개일 수 있으면 List로 받는다
	//   (Product 단건으로 받으면 2개 이상일 때 IncorrectResultSizeDataAccessException)
	List<Product> findByNameNot(String name); // where name <> ?

	List<Product> findByNameAndPriceLessThan(String name, Integer price); // where name = ? and price < ? — 인수 순서 = 이름 속 칸 순서. 타입은 필드 타입(Integer)에 맞춤

	List<Product> findByPriceBetween(Integer low, Integer high); // where price between ? and ? — 양 끝(low, high) 포함

	List<Product> findByNameContaining(String keyword); // where name like '%keyword%' — 앞뒤 %는 스프링이 붙여 줌

	// 4-3: 같은 조건에 정렬 · 페이지 인수를 붙인 판. 이름이 같아도 인수가 다르면 다른 메서드(오버로딩)
	List<Product> findByNameContaining(String keyword, Sort sort); // 정렬 기준을 부르는 쪽이 정함 → order by ...

	Page<Product> findByNameContaining(String keyword, Pageable pageable); // 몇 번째 페이지 · 몇 개씩 · 정렬을 한 번에. 내용 쿼리 + count 쿼리 두 번 나감

	// 4-4 ①: JPQL — 테이블 · 컬럼이 아니라 엔티티 이름(Product)과 필드 이름(p.price)으로 쓴다
	@Query("select p from Product p where p.price >= :min order by p.price desc") // :min 자리에 @Param("min") 값이 들어감. 이름이 다르면 시작할 때 실패
	List<Product> findExpensive(@Param("min") int min);

	// 4-4 ②: 필요한 칸만 — 인터페이스 프로젝션. 돌려받는 타입만 ProductSummary로 바꾸면 select name, price만 나감
	//   (find와 By 사이의 Summary는 아무 단어나 됨. 결과 모양은 돌려받는 타입이 정함)
	List<ProductSummary> findSummaryByNameContaining(String keyword);

	// 4-4 ③ · ④: 한 번에 여러 행 수정 (벌크 수정)
	//   @Modifying: select가 아닌 쿼리(update · delete)라는 표시. 빠뜨리면 "must be a 'select' query" 오류
	//   clearAutomatically = true: 끝나면 영속성 컨텍스트를 비움 → 다음 조회는 DB에서 새로 읽음. 빠뜨리면 앞에서 읽어 둔 옛 값이 보임
	@Modifying(clearAutomatically = true)
	@Query("update Product p set p.price = p.price + :amount where p.name = :name")
	int raiseAll(@Param("name") String name, @Param("amount") int amount); // 돌려주는 int = 바뀐 행 수
}
