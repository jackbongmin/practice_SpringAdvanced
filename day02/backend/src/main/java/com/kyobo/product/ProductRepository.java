package com.kyobo.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 1일차 (10_01) · 가져옴
 *
 * [역할] 상품 테이블을 읽고 쓰는 저장소. JpaRepository가 save · findById · delete 등을 만들어 줌
 *
 * [알아야 할 것]
 * - 인터페이스만 쓰면 스프링 데이터 JPA가 구현을 만들어 줌. findByName은 메서드 이름으로 쿼리를 만듦 (10_03에서 자세히)
 * - 서비스 단위 테스트에서는 mock(ProductRepository.class)로 가짜를 넣음
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findByName(String name);
}
