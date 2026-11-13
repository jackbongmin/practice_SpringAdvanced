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
}
