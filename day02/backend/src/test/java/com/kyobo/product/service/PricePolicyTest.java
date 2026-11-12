package com.kyobo.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 2일차 · 02 (커버리지 · TDD · AI 테스트 초안 검토)
 *
 * [역할] PricePolicy.raisePrice의 규칙을 적은 테스트. TDD에서 "코드보다 먼저" 쓰는 파일
 *   - 기본: 1000원 10% → 1100원, 인상률 0 → 그대로
 *   - 경계: 원 단위 반올림 (1357.4 → 1357, 1105.5 → 1106, 1897.5 → 1898)
 *   - 거절: 음수 인상률 · 음수 가격 → IllegalArgumentException
 *
 * [알아야 할 것]
 * - 스프링 없이 도는 단위 테스트. PricePolicy는 계산만 하므로 new로 만들어 씀 (@SpringBootTest 필요 없음)
 * - @ParameterizedTest + @CsvSource: 값만 다른 테스트를 한 메서드로 여러 번 돌림. 줄 하나 = 실행 한 번
 * - 빨강 → 초록 → 리팩터링 순서로 이 파일과 PricePolicy를 번갈아 고침 (가이드 4-4 할 일 ①~④)
 * - 끝나면 테스트 7개(매개변수 3건 포함)
 */
class PricePolicyTest {

	PricePolicy policy = new PricePolicy(); // 계산만 하는 클래스라 스프링 없이 new로 만듦

	@Test
	void 천원을_10퍼센트_올리면_1100원() {
		assertThat(policy.raisePrice(1000, 10)).isEqualTo(1100);
	}

	@Test
	void 인상률_0이면_그대로() {
		assertThat(policy.raisePrice(1234, 0)).isEqualTo(1234);
	}

	@ParameterizedTest // 값만 다른 테스트를 여러 번 돌림
	@CsvSource({       // 줄 하나 = 실행 한 번. 순서대로 price, percent, expected에 들어감
			"1234, 10, 1357", // 1357.4 → 내림
			"1005, 10, 1106", // 1105.5 → 올림
			"1650, 15, 1898"  // 1897.5 → 올림 (double로 계산하면 1897이 나와 실패했던 값)
	})
	void 원_단위_반올림(int price, int percent, int expected) {
		assertThat(policy.raisePrice(price, percent)).isEqualTo(expected);
	}

	@Test
	void 음수_인상률은_거절() {
		assertThatThrownBy(() -> policy.raisePrice(1000, -10)) // 람다 안의 코드가 예외를 던져야 통과
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 음수_가격은_거절() {
		assertThatThrownBy(() -> policy.raisePrice(-1000, 10))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
