package com.kyobo.product;

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
 * - 지금은 빈 클래스. raisePrice가 없으니 PricePolicyTest를 먼저 쓰면 컴파일 실패가 남 → 그것이 첫 번째 빨강
 * - 금액을 double로 계산하면 1650 * 1.15 = 1897.4999... 가 되어 반올림이 틀림 → BigDecimal로 계산함
 * - 계산만 하는 순수한 클래스라 테스트에서 mock 없이 new PricePolicy()로 바로 씀
 * - ProductService가 생성자로 받아 씀 (4-4 할 일 ⑤에서 연결)
 */
public class PricePolicy {

}
