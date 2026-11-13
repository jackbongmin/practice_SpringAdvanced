package com.kyobo.product.dto;

/**
 * 3일차 · 03 (쿼리 메서드 · 정렬 · 페이지 · JPQL)
 *
 * [역할] 상품의 이름 · 가격 두 칸만 담는 "인터페이스 프로젝션". 저장소가 이 모양으로 돌려주면 select에 두 칸만 들어감
 *
 * [알아야 할 것]
 * - 구현 클래스를 만들지 않음. 스프링 데이터가 getName() · getPrice()를 보고 같은 이름의 엔티티 필드만 읽어 채워 줌
 * - getter 이름이 엔티티 필드와 달라야 하면(getTitle 등) 값을 못 찾음 → 필드 이름(name, price)에 맞춤
 * - 가이드 03 4-4 할 일 ②에서 채움
 */
public interface ProductSummary {

}
