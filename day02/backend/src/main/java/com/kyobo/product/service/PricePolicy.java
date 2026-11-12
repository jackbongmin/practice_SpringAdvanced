package com.kyobo.product.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * 2일차 · 02 (커버리지 · TDD · AI 테스트 초안 검토)
 *
 * [역할] 가격 계산 규칙만 맡는 클래스. "가격을 n% 올린 값"을 계산해 돌려줌 (DB · 웹을 모름)
 *   - raisePrice(price, percent) : 원 단위 반올림(0.5원은 올림). 음수 가격 · 음수 인상률은 IllegalArgumentException
 *
 * [알아야 할 것]
 * - TDD로 만듦: PricePolicyTest를 "먼저" 쓰고(빨강) → 가장 단순하게 통과시키고(초록) → 고침(리팩터링). 가이드 4-4
 * - 처음엔 빈 클래스였음. raisePrice가 없을 때 PricePolicyTest를 먼저 쓰면 컴파일 실패가 남 → 그것이 첫 번째 빨강
 * - 금액을 double로 계산하면 1650 * 1.15 = 1897.4999... 가 되어 반올림이 틀림 → BigDecimal로 계산함
 * - 계산만 하는 순수한 클래스라 테스트에서 mock 없이 new PricePolicy()로 바로 씀
 * - ProductService가 생성자로 받아 씀 (4-4 할 일 ⑤에서 연결)
 */
@Component // 스프링이 이 클래스를 빈으로 만들어 둠 → ProductService가 생성자로 받을 수 있음 (4-4 ⑤)
public class PricePolicy {

	// 원 단위 반올림(0.5는 올림). 예: 1650원 15% → 1897.5 → 1898
	public int raisePrice(int price, int percent) {
		if (price < 0 || percent < 0) {
			throw new IllegalArgumentException("가격과 인상률은 0 이상이어야 합니다"); // 음수 거절 → 테스트 2개 통과
		}
		// double(price * 1.15)은 1897.4999…가 되어 1897로 내려간다 → BigDecimal로 정확히 계산
		return BigDecimal.valueOf(price)
				.multiply(BigDecimal.valueOf(100 + percent))              // 1650 × 115 = 189750
				.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP) // ÷ 100, 소수 0자리, 0.5는 올림 → 1898
				.intValueExact();                                         // int로. int 범위를 넘으면 예외 (몰래 잘리지 않음)
	}
}
