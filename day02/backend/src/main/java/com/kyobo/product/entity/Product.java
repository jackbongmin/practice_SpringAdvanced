package com.kyobo.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * 1일차 (10_01) · 가져옴 → 2일차 · 02에서 changePrice 추가
 *
 * [역할] 상품 한 건(번호 · 이름 · 가격 · 재고)을 담는 JPA 엔티티. 테이블 product의 한 줄과 1:1
 *
 * [알아야 할 것]
 * - number는 DB가 붙임(IDENTITY). setter가 없어서 테스트에서는 ReflectionTestUtils로 넣음 (ProductServiceTest.saved)
 * - 값을 바꾸는 메서드(changeName, changePrice)만 열어 둠. @Transactional 안에서 바꾸면 저장 호출 없이 UPDATE가 나감(변경 감지)
 * - 가이드 4-4 할 일 ⑤에서 changePrice(int price)를 더함
 */
@Entity
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long number;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Integer price;

	@Column(nullable = false)
	private Integer stock;

	protected Product() {
	}

	public Product(String name, int price, int stock) {
		this.name = name;
		this.price = price;
		this.stock = stock;
	}

	public Long getNumber() { return number; }
	public String getName() { return name; }
	public Integer getPrice() { return price; }
	public Integer getStock() { return stock; }

	public void changeName(String name) {
		this.name = name;
	}

	// 4-4: 가격 변경. @Transactional 안에서 부르면 save 없이도 UPDATE가 나감 (변경 감지)
	public void changePrice(int price) {
		this.price = price;
	}
}
