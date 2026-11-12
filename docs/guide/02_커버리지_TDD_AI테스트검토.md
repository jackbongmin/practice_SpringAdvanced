# 가이드 02 · 커버리지 · TDD · AI 테스트 초안 검토

> **한 줄 요약**: 1일차(10_01)에 만든 상품 API 테스트에 **JaCoCo**를 붙여 테스트가 코드의 **어느 줄을 실행했는지** 재고, 빈 곳을 채우고, **테스트를 먼저 쓰는 방식(TDD)**으로 "가격 n% 인상" 기능을 만듦. 마지막으로 AI가 써 준 테스트 초안이 왜 믿을 수 없는지 표로 검토함

## 목표
- `./gradlew test` 한 번으로 **커버리지 보고서(HTML)**가 나오게 하고, 초록 · 빨강 · 노랑 줄을 읽을 수 있음
- **단언 없는 테스트**가 커버리지만 올리고 버그는 못 잡는 것을 직접 봄 → "커버리지 = 실행했나, 단언 = 맞았나"를 설명할 수 있음
- "서비스 줄 커버리지 90% 미만이면 빌드 실패" 규칙을 걸고, 테스트를 더해 통과시킴
- `PricePolicy`를 **빨강 → 초록 → 리팩터링** 순서로 만듦. `double`로 금액을 계산하면 왜 틀리는지 봄
- AI 테스트 초안을 **버전 · 이름 · 기대값** 기준으로 검토함

## 완료 기준
| 확인 | 오늘 시작할 때 | 오늘 끝났을 때 |
|---|---|---|
| `.\gradlew.bat test` | 테스트 10개 통과, 커버리지 보고서 없음 | 테스트 **20개** 통과 + `build\reports\jacoco\test\html\index.html` 생김 |
| `.\gradlew.bat check` | 테스트만 돎 | 테스트 + 커버리지 규칙(`ProductService` · `PricePolicy` 줄 90% 이상) 통과 → `BUILD SUCCESSFUL` |
| `ProductService` 줄 커버리지 | `0.58` | `1.00` |
| `PUT /product/price?number=1&percent=15` (1650원 상품) | `404` (주소 없음) | `200 {"number":1,"name":"pen","price":1898,"stock":10}` |
| `PUT /product/price?number=1&percent=-5` | `404` | `500` (막긴 했지만 400으로 바꾸는 곳이 없음. 6장) |

- [ ] 원본 `backend-final`의 테스트 20개와 같은 결과 (Claude Code가 가이드 코드를 scratchpad에 단계대로 적용해 20개 통과 · `check` 통과 확인함)

## 만들 API
| 요청 | 설명 |
|---|---|
| `PUT /product/price?number=1&percent=15` | 상품 가격을 15% 올림. 원 단위 반올림(0.5원은 올림). 응답은 바뀐 상품 |
| 1일차 API | `GET /product?number=1` · `POST /product` · `PUT /product`(이름 변경) · `DELETE /product?number=1` 그대로 |

응답 예시:
```json
// POST /product   {"name":"pen","price":1650,"stock":10}
{"number":1,"name":"pen","price":1650,"stock":10}
// PUT /product/price?number=1&percent=15
{"number":1,"name":"pen","price":1898,"stock":10}
```

---

## 1. 개념 먼저

### 1-1. 커버리지: "테스트가 어디를 지나갔나"
- 비유: 건물 점검표. 점검원이 **들어가 본 방**에 표시함. 표시가 없는 방은 아무도 안 본 방
- 뜻: 테스트를 돌리는 동안 **실행된 코드 줄의 비율**. 도구는 **JaCoCo**(Java Code Coverage)
- 오늘 코드에서: `ProductService`의 `changeName` 본문과 `deleteProduct`의 `products.delete(...)` 줄이 **한 번도 실행 안 됨** → 보고서에 빨강 (4-1)

| 색 | 뜻 |
|---|---|
| 초록 | 테스트가 실행한 줄 |
| 빨강 | 한 번도 실행 안 된 줄 |
| 노랑 | `if` 같은 갈림길에서 **한쪽만** 실행된 줄 |

### 1-2. 커버리지의 함정: 실행했다 ≠ 맞았다
- 점검원이 방에 **들어가기만 하고 아무것도 안 봐도** 점검표에는 표시됨
- 테스트도 같음. 메서드를 부르기만 하고 `assertThat(...)`(단언)이 없으면 줄은 초록이 되지만, **결과가 틀려도 실패하지 않음** (4-2에서 직접 봄)
- 그래서 커버리지는 **"테스트가 없는 곳을 찾는 도구"**로 씀. 숫자 자체를 목표로 삼으면 단언 없는 테스트가 늘어남

### 1-3. 커버리지 규칙: 숫자 아래로 내려가면 빌드 실패
- `jacocoTestCoverageVerification`: "이 클래스는 줄 커버리지 90% 이상"처럼 **최소선**을 정함
- `./gradlew test`는 규칙을 **보지 않음.** `./gradlew check`(또는 `build`)가 규칙을 검사함
- 실무에서는 이 검사를 CI(4-6)에 걸어 **테스트 없이 코드가 들어오는 것**을 막음

### 1-4. TDD: 테스트를 먼저 씀
| 순서 | 이름 | 하는 일 |
|---|---|---|
| ① | 빨강 (Red) | 원하는 동작을 **테스트로 먼저** 적음. 코드가 없으니 실패(컴파일 실패도 빨강) |
| ② | 초록 (Green) | 테스트를 통과하는 **가장 단순한** 코드를 씀 |
| ③ | 리팩터링 (Refactor) | 테스트가 초록인 채로 코드를 정리함. 테스트가 안전망 |
- 오늘은 경계값 테스트를 더해 **다시 빨강**을 만들고(①→②→①→②③) `double` 반올림 버그를 찾음 (4-4)

### 1-5. 금액은 `double`로 계산하지 않음
- `double`은 2진수라 `0.1`, `1.15` 같은 수를 **정확히 담지 못함**
- `1650 * 1.15` = `1897.4999999999998` → 반올림하면 `1897` (기대값은 `1898`)
- `BigDecimal`은 10진수를 그대로 계산함 → `1650 × 115 ÷ 100` = `1897.5` → `HALF_UP` 반올림 → `1898`

### 1-6. 용어 정리
| 용어 | 쉬운 뜻 |
|---|---|
| JaCoCo | 테스트가 실행한 줄을 세어 보고서(HTML)를 만드는 도구 |
| 줄 커버리지 (LINE) | 전체 실행 가능한 줄 중 테스트가 실행한 줄의 비율 |
| 단언 (assertion) | `assertThat(실제).isEqualTo(기대)`처럼 결과가 맞는지 검사하는 줄 |
| `check` 태스크 | 테스트 + 검사(오늘은 커버리지 규칙)를 모두 돌리는 Gradle 명령 |
| TDD | 테스트 먼저 → 통과하는 코드 → 정리. 빨강 · 초록 · 리팩터링 |
| `@ParameterizedTest` | 값만 다른 테스트를 한 메서드로 여러 번 돌림 |
| `@CsvSource` | 그 값을 `"1234, 10, 1357"`처럼 쉼표로 적는 방법. 줄 하나 = 실행 한 번 |
| `BigDecimal` · `HALF_UP` | 10진수 정확 계산 클래스 · 0.5 이상은 올리는 반올림 |
| mock | 진짜 대신 넣는 가짜 객체. 대답을 정하지 않으면 `0` · `null`을 돌려줌 |
| CI | push할 때마다 다른 컴퓨터가 테스트를 자동으로 돌리는 것 |

---

## 2. 미리 바꿔둔 설정

### 2-1. 1일차에서 가져온 파일
이 저장소는 2일차부터 시작함. 그래서 **1일차(10_01) 완성 코드**(원본의 `10_02/backend-start`)를 `day02/backend`로 가져와 둠.

| 파일 | 상태 | 오늘 할 일 |
|---|---|---|
| `ProductApplication` · `ProductDtos` · `ProductNotFoundException` · `ProductRepository` | 1일차 코드 그대로 (역할 주석만 더함) | **안 고침** |
| `Product` | 1일차 코드 그대로 | `changePrice()` 추가 (4-4 ⑤) |
| `ProductService` | 1일차 코드 그대로 | 4-2에서 한 줄 잠깐 주석 처리 / 생성자 · `raisePrice()` 추가 (4-4 ⑤) |
| `ProductController` | 1일차 코드 그대로 | `raisePrice()` 추가 (4-4 ⑤) |
| `ProductServiceTest` | 1일차 테스트 4개 | 테스트 3개 추가 + `setUp()` 변경 (4-2 · 4-3 · 4-4 ⑤) |
| `ProductControllerTest` · `ProductRepositoryTest` | 1일차 테스트 각 3개 | **안 고침** |
| `PricePolicy` | **빈 클래스** (import · 역할 주석만) | **새로 씀**: 4-4 |
| `PricePolicyTest` | **빈 클래스** (import · 역할 주석만) | **새로 씀**: 4-4 (코드보다 먼저) |

- 가져온 클래스는 코드는 그대로 두고 맨 위의 한 줄 `//` 주석만 **역할 주석**으로 바꿈
- `PricePolicy`는 빈 클래스로 있어서, 4-4 ①의 첫 빨강은 "클래스가 없음"이 아니라 **"`raisePrice` 메서드가 없음"**으로 나옴

### 2-2. 설정 파일
- `build.gradle` · `application.properties`: **안 바꿈.** JaCoCo를 붙이는 것 자체가 오늘 실습이라 4-1 · 4-3에서 직접 고침
- `settings.gradle`: 프로젝트 이름 `product-coverage` (원본 그대로)

### 2-3. IntelliJ
- `day02/backend/build.gradle`을 Gradle 프로젝트로 연결함 → [README "IntelliJ에서 Gradle 프로젝트 연결하기"](README.md#intellij에서-gradle-프로젝트-연결하기)
- 4-1에서 `build.gradle`을 고친 뒤에는 Gradle 탭 **Reload**

---

## 3. 실행 방법과 확인 도구
4장을 시작하기 전에 한 번 읽어 둠. 오늘은 **화면이 없음.** 확인은 테스트 결과 · 커버리지 보고서 · curl로 함.

### 3-1. 테스트 · 검사 돌리기
```powershell
# 저장소 루트에서
cd day02\backend
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.12"
.\gradlew.bat test      # 테스트만
.\gradlew.bat check     # 테스트 + 커버리지 규칙 (4-3부터 의미 있음)
```
- 테스트 이름과 결과가 한 줄씩 나옴: `ProductServiceTest > 없는_번호는_삭제하지_않는다() PASSED`
- 처음 한 번은 의존성 내려받기로 1~2분 걸림
- IntelliJ에서 테스트 클래스 옆 ▶로 돌려도 되지만, **커버리지 규칙은 터미널 `check`로** 확인함

### 3-2. 보고서 두 가지
| 보고서 | 경로 (`day02\backend\` 아래) | 무엇 |
|---|---|---|
| 테스트 결과 | `build\reports\tests\test\index.html` | 어떤 테스트가 통과 · 실패했나, 실패 메시지 |
| 커버리지 | `build\reports\jacoco\test\html\index.html` | 어떤 줄을 실행했나 (4-1부터 생김) |
- 탐색기에서 더블클릭하거나 PowerShell에서 `start build\reports\jacoco\test\html\index.html`
- 커버리지 보고서 읽는 순서: `com.kyobo.product` → `ProductService` → 위쪽 `ProductService.java` 링크를 누르면 **줄마다 색**이 보임
- 테스트를 다시 돌린 뒤에는 브라우저에서 `F5`

### 3-3. 서버 켜고 curl (4-4 ⑤에서만)
```powershell
# 터미널 1 — 서버
cd day02\backend
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.12"
.\gradlew.bat bootRun
```
```powershell
# 터미널 2 — 요청
curl.exe -X POST http://localhost:8080/product -H "Content-Type: application/json" -d '{\"name\":\"pen\",\"price\":1650,\"stock\":10}'
curl.exe -X PUT "http://localhost:8080/product/price?number=1&percent=15"
```
- 서버가 뜬 신호: `Started ProductApplication`
- PowerShell 5.1의 `curl.exe`에서는 JSON 안 따옴표를 `\"`로 씀
- 이름은 영어(`pen`)로 보냄. 한글을 터미널에서 보내면 인코딩 때문에 `400`이 날 수 있음
- H2 **메모리 DB**라서 서버를 끄면 상품이 사라짐 → 켤 때마다 등록부터

---

## 4. 단계별 진행
- 작업 폴더: `day02/backend`
- 단계마다 맨 위에 **목적**(왜)과 **수정**(어느 파일의 어디를)을 적었음. 먼저 읽고 코드를 씀
- 결과가 다르면 다음 단계로 가지 말고 5장 "막히면"을 봄

```
day02/backend/
├── build.gradle                                  (4-1, 4-3, 4-4 ⑤에서 고침)
└── src/
    ├── main/java/com/kyobo/product/
    │   ├── ProductApplication · ProductDtos · ProductNotFoundException · ProductRepository  (그대로)
    │   ├── Product.java              (4-4 ⑤ changePrice 추가)
    │   ├── ProductService.java       (4-2 한 줄 주석 처리했다 되돌림 / 4-4 ⑤ raisePrice 추가)
    │   ├── ProductController.java    (4-4 ⑤ PUT /product/price 추가)
    │   └── PricePolicy.java          (빈 클래스 → 4-4에서 씀)
    └── test/java/com/kyobo/product/
        ├── ProductControllerTest · ProductRepositoryTest   (그대로)
        ├── ProductServiceTest.java   (4-2, 4-3, 4-4 ⑤에서 테스트 추가)
        └── PricePolicyTest.java      (빈 클래스 → 4-4에서 "먼저" 씀)
```

| 단계 | 할 일 | 보게 될 것 |
|---|---|---|
| 4-0 | 출발점 (코드 없음) | 테스트 10개 통과 |
| 4-1 | JaCoCo 붙이기 | 보고서에서 `changeName` 본문 · `delete` 줄 빨강 |
| 4-2 | 커버리지의 함정 | 단언 없는 테스트 → 기능을 지워도 통과 |
| 4-3 | 커버리지 규칙 | `0.83` 미달로 실패 → 삭제 테스트 더해 통과 |
| 4-4 | TDD — 가격 인상 | 컴파일 실패 → 2개 통과 → 3개 실패 → 7개 통과 → API 연결 |
| 4-5 | AI 테스트 초안 검토 (코드 없음) | 표 채우기 |
| 4-6 | (엑스트라) GitHub Actions CI | push하면 초록 체크 / 일부러 깨면 빨간 X |

---

### 4-0. 출발점 (코드 없음)

> **목적**: 가져온 1일차 코드가 잘 도는지 보고, "이 테스트들이 서비스 코드의 **몇 줄**을 실행했을까?"를 질문으로 남김
>
> **수정**: 없음

#### 할 일
3-1대로 `.\gradlew.bat test`

#### 확인
```
ProductControllerTest > GET_product_번호로_조회() PASSED
ProductControllerTest > POST_product_JSON_본문이_요청_객체로_바뀐다() PASSED
ProductControllerTest > 없는_상품은_404() PASSED
ProductRepositoryTest > 저장하면_번호가_붙고_다시_찾을_수_있다() PASSED
ProductRepositoryTest > 이름으로_찾기() PASSED
ProductRepositoryTest > 각_테스트는_빈_DB에서_시작한다() PASSED
ProductServiceTest > 없는_번호를_조회하면_ProductNotFoundException() PASSED
ProductServiceTest > 저장할_때_요청_값이_그대로_저장소에_넘어간다() PASSED
ProductServiceTest > 없는_번호는_삭제하지_않는다() PASSED
ProductServiceTest > 번호로_조회하면_저장소의_상품을_응답으로_바꾼다() PASSED

BUILD SUCCESSFUL
```
- [ ] 10개 통과 (서비스 4 · 컨트롤러 3 · 저장소 3). 빈 `PricePolicyTest`는 테스트가 0개라 목록에 안 나옴

---

### 4-1. JaCoCo 붙이기

> **목적**: 테스트가 끝나면 커버리지 보고서가 자동으로 나오게 함 (1-1)
>
> **수정**: `build.gradle` — `plugins`에 한 줄, `tasks.named('test')`에 한 줄, 블록 두 개 추가

#### 할 일
`build.gradle`의 `plugins { }` 안 맨 아래에 추가
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '3.3.13'
	id 'io.spring.dependency-management' version '1.1.7'
	id 'jacoco' // 4-1: 커버리지 측정 플러그인. 테스트가 실행한 줄을 기록함
}
```
`tasks.named('test') { }`를 아래처럼 바꾸고, 그 아래에 블록 두 개 추가
```groovy
tasks.named('test') {
	useJUnitPlatform()
	testLogging { events 'passed', 'failed' } // 테스트 이름과 결과를 콘솔에 보인다
	finalizedBy 'jacocoTestReport'            // 4-1: 테스트가 끝나면 보고서를 만든다. 빠뜨리면 build/reports/jacoco가 안 생김
}

jacoco {
	toolVersion = '0.8.12' // JaCoCo 버전
}

// 4-1: build/reports/jacoco/test/html/index.html
jacocoTestReport {
	dependsOn test // 보고서는 테스트 결과(실행 기록)가 있어야 만들 수 있음
}
```
IntelliJ Gradle 탭 **Reload** → `.\gradlew.bat test` → 커버리지 보고서를 엶 (3-2)

#### 확인
- `build\reports\jacoco\test\html\index.html`이 생김
- `com.kyobo.product` → `ProductService` → `ProductService.java`:

| 메서드 | 색 |
|---|---|
| `getProduct` · `saveProduct` · `find` | 초록 |
| `changeName` 본문 (`find` · `product.changeName(name)` · `return`) | **빨강** |
| `deleteProduct`의 `products.delete(find(number));` | **빨강** — 없는 상품 삭제만 테스트해서 `find`에서 예외가 나고 끝남. JaCoCo는 예외로 중간에 끊긴 줄을 "실행 안 됨"으로 침 |

- [ ] 보고서가 열리고 `changeName` 본문이 빨강

> 테스트 10개가 전부 초록이었는데도 **이름 변경 기능은 한 번도 실행된 적이 없었음.** "테스트가 다 통과한다"와 "다 테스트했다"는 다른 말임
>
> 교재 원본 `pom.xml`은 `ProductServiceImpl`을 측정에서 **빼 둠**(`<exclude>`). 가장 중요한 클래스가 보고서에 안 나오는 설정이니 주의

---

### 4-2. 커버리지의 함정

> **목적**: 단언 없는 테스트가 커버리지만 올리고 버그는 못 잡는 것을 직접 봄 (1-2)
>
> **수정**: `ProductServiceTest.java` — 테스트 하나 추가 → `ProductService.java` 한 줄 잠깐 주석 처리 → 테스트를 단언 있는 것으로 바꿈 → 주석 되돌림

#### 할 일 ①: 단언 없는 테스트
`ProductServiceTest.java` 맨 아래(마지막 `}` 위)에 추가
```java
	// 단언 없는 테스트 — 커버리지만 올린다 (일부러 나쁜 예)
	@Test
	void 이름_변경_실행만() {
		given(products.findById(1L)).willReturn(Optional.of(saved(1L, "펜", 1000, 10))); // 가짜 저장소에 "1번은 펜"이라고 대답을 정해 둠
		service.changeName(1L, "볼펜"); // 부르기만 하고 결과를 확인하지 않음
	}
```
`.\gradlew.bat test` → 커버리지 보고서 `F5`

#### 확인 ①
- 테스트 **11개** 통과
- `changeName` 본문이 **초록**이 됨

#### 할 일 ②: 기능을 지워 봄
`ProductService.java`의 `changeName` 안에서 이 줄을 주석 처리
```java
		// product.changeName(name);
```
`.\gradlew.bat test`

#### 확인 ②
- 테스트 **11개 전부 통과** (`BUILD SUCCESSFUL`)
- [ ] 이름 변경 기능이 없어졌는데 아무 테스트도 실패하지 않음

#### 할 일 ③: 단언이 있는 테스트로 바꿈
`이름_변경_실행만` 테스트를 지우고 아래로 바꿈 (`product.changeName(name);`은 **아직 주석인 채로** 둠)
```java
	@Test
	void 이름을_바꾸면_응답에_새_이름() {
		given(products.findById(1L)).willReturn(Optional.of(saved(1L, "펜", 1000, 10)));

		ProductResponse res = service.changeName(1L, "볼펜");

		assertThat(res.name()).isEqualTo("볼펜"); // 단언: 응답의 이름이 정말 바뀌었나
	}
```
`.\gradlew.bat test`

#### 확인 ③
```
ProductServiceTest > 이름을_바꾸면_응답에_새_이름() FAILED
11 tests completed, 1 failed
BUILD FAILED
```
- 테스트 결과 보고서(3-2)의 실패 메시지:
  ```
  expected: "볼펜"
   but was: "펜"
  ```

#### 할 일 ④: 반드시 되돌림
`ProductService.java`의 `// product.changeName(name);` 주석을 풀고 `.\gradlew.bat test` → 11개 통과
- [ ] 주석을 풀었고 11개 통과

> 커버리지는 "실행했는가"만 잼. "결과가 맞는가"는 **단언**이 잼. 같은 줄을 실행해도 단언이 있어야 버그를 잡음

---

### 4-3. 커버리지 규칙

> **목적**: "서비스 줄 커버리지 90% 미만이면 빌드 실패" 규칙을 걸고, 모자란 테스트를 채워 통과시킴 (1-3)
>
> **수정**: `build.gradle` — 블록 두 개 추가 / `ProductServiceTest.java` — 테스트 하나 추가

#### 할 일 ①: 규칙 추가
`build.gradle` 맨 아래에 추가
```groovy
// 4-3: 서비스는 줄 커버리지 90% 미만이면 빌드 실패 (4-4 ⑤에서 PricePolicy도 넣음)
jacocoTestCoverageVerification {
	violationRules {
		rule {
			element = 'CLASS'                                  // 클래스마다 따로 잼
			includes = ['com.kyobo.product.ProductService']    // 이 클래스만 검사
			limit {
				counter = 'LINE'                               // 줄 기준
				minimum = 0.90                                 // 90% 미만이면 실패
			}
		}
	}
}

tasks.named('check') {
	dependsOn jacocoTestCoverageVerification // check를 돌리면 규칙도 검사함. 빠뜨리면 규칙이 있어도 아무 일 없음
}
```
`.\gradlew.bat check`

#### 확인 ①
```
> Task :jacocoTestCoverageVerification FAILED
Rule violated for class com.kyobo.product.ProductService: lines covered ratio is 0.83, but expected minimum is 0.90
BUILD FAILED
```
- 4-2의 이름 변경 테스트가 있는 상태라 `0.83`. 4-0 상태(그 테스트 없음)였다면 `0.58` (Claude Code 실측)
- 남은 빨간 줄: `deleteProduct`의 `products.delete(...)`
- [ ] `0.83 ... expected minimum is 0.90`으로 실패

#### 할 일 ②: 있는 상품 삭제 테스트
`ProductServiceTest.java` 맨 아래에 추가
```java
	@Test
	void 있는_상품은_삭제한다() {
		Product pen = saved(1L, "펜", 1000, 10);
		given(products.findById(1L)).willReturn(Optional.of(pen));

		service.deleteProduct(1L);

		verify(products).delete(pen); // 단언: 저장소의 delete가 "그 펜"으로 불렸나
	}
```
`.\gradlew.bat check`

#### 확인 ②
- 테스트 **12개** 통과, `BUILD SUCCESSFUL`
- 커버리지 보고서에서 `ProductService`가 전부 초록 (`1.00`)
- [ ] `check` 통과

> `0.58` → 이름 변경 테스트 `0.83` (단언이 **있든 없든** 같은 숫자) → 삭제 테스트까지 `1.00`. 숫자는 단언을 구별 못 함 → 숫자만 맞추는 테스트를 막는 것은 **코드 리뷰**임
>
> `.\gradlew.bat test`는 규칙을 안 봄. 규칙은 `check`(또는 `build`)에서 검사함

---

### 4-4. TDD — 가격 인상

> **목적**: 요구사항 **"상품 가격을 n% 올린다. 원 단위 반올림(0.5원은 올림). 음수 인상률 · 음수 가격은 거절"**을 테스트 먼저 쓰는 방식으로 만듦 (1-4, 1-5)
>
> **수정**: `PricePolicyTest.java` → `PricePolicy.java`를 번갈아 ①~④ / ⑤에서 `Product` · `ProductService` · `ProductController` · `ProductServiceTest` · `build.gradle` 연결

#### 할 일 ①: 빨강 — 테스트 먼저
빈 `PricePolicyTest.java`의 클래스 안을 채움 (import · 역할 주석은 이미 있음)
```java
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
}
```
`.\gradlew.bat test`

#### 확인 ①: 컴파일 실패
```
PricePolicyTest.java:...: error: cannot find symbol
  symbol:   method raisePrice(int,int)
  location: variable policy of type PricePolicy
BUILD FAILED
```
- [ ] `raisePrice`가 없다는 컴파일 실패 — **이것도 빨강**

#### 할 일 ②: 초록 — 가장 단순하게
빈 `PricePolicy.java`의 클래스를 채움
```java
@Component // 스프링이 이 클래스를 빈으로 만들어 둠 → ProductService가 생성자로 받을 수 있음 (4-4 ⑤)
public class PricePolicy {

	public int raisePrice(int price, int percent) {
		return (int) Math.round(price * (1 + percent / 100.0)); // 일부러 double로 계산 (가장 단순한 방법)
	}
}
```
`.\gradlew.bat test`

#### 확인 ②
```
PricePolicyTest > 천원을_10퍼센트_올리면_1100원() PASSED
PricePolicyTest > 인상률_0이면_그대로() PASSED
BUILD SUCCESSFUL
```
- [ ] 2개 통과 (전체 14개)

#### 할 일 ③: 다시 빨강 — 경계값 · 음수
`PricePolicyTest.java`의 두 테스트 아래에 추가
```java
	@ParameterizedTest // 값만 다른 테스트를 여러 번 돌림
	@CsvSource({       // 줄 하나 = 실행 한 번. 순서대로 price, percent, expected에 들어감
			"1234, 10, 1357", // 1357.4 → 내림
			"1005, 10, 1106", // 1105.5 → 올림
			"1650, 15, 1898"  // 1897.5 → 올림
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
```
`.\gradlew.bat test`

#### 확인 ③: 3개 실패
```
PricePolicyTest > 음수_가격은_거절() FAILED
PricePolicyTest > 천원을_10퍼센트_올리면_1100원() PASSED
PricePolicyTest > 인상률_0이면_그대로() PASSED
PricePolicyTest > 음수_인상률은_거절() FAILED
PricePolicyTest > 원_단위_반올림(int, int, int) > [1] price=1234, percent=10, expected=1357 PASSED
PricePolicyTest > 원_단위_반올림(int, int, int) > [2] price=1005, percent=10, expected=1106 PASSED
PricePolicyTest > 원_단위_반올림(int, int, int) > [3] price=1650, percent=15, expected=1898 FAILED
19 tests completed, 3 failed
BUILD FAILED
```
- 테스트 결과 보고서(3-2)의 실패 메시지
  - `[3] price=1650 ...` → `expected: 1898` / `but was: 1897`
  - `음수_인상률은_거절` · `음수_가격은_거절` → `Expecting code to raise a throwable.` (예외가 안 났음)
- [ ] 3개 실패, 그중 하나는 `1650, 15`

> `1650 * 1.15`는 `double`로 `1897.4999999999998`. 0.5가 안 돼서 내려감. **1005원은 맞고 1650원은 틀림** → 예시 하나만 테스트했다면 못 찾았을 버그

#### 할 일 ④: 초록 → 리팩터링 — `BigDecimal`
`raisePrice`를 바꿈
```java
	// 원 단위 반올림(0.5는 올림). 예: 1650원 15% → 1897.5 → 1898
	public int raisePrice(int price, int percent) {
		if (price < 0 || percent < 0) {
			throw new IllegalArgumentException("가격과 인상률은 0 이상이어야 합니다"); // 음수 거절 → 테스트 2개 통과
		}
		// double(price * 1.15)은 1897.4999…가 되어 1897로 내려간다 → BigDecimal로 정확히 계산
		return BigDecimal.valueOf(price)
				.multiply(BigDecimal.valueOf(100 + percent))            // 1650 × 115 = 189750
				.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP) // ÷ 100, 소수 0자리, 0.5는 올림 → 1898
				.intValueExact();                                       // int로. int 범위를 넘으면 예외 (몰래 잘리지 않음)
	}
```
`.\gradlew.bat test`

#### 확인 ④
- `PricePolicyTest` **7개** 통과 (매개변수 3건 포함), 전체 19개, `BUILD SUCCESSFUL`
- [ ] 7개 통과

> 계산 방식을 통째로 바꿨는데도 **테스트가 그대로 초록** → 리팩터링이 안전했다는 증거. 이것이 테스트를 먼저 써 두는 이유

#### 할 일 ⑤: 서비스 · 컨트롤러에 연결
**`Product.java`** — `changeName()` 아래에 추가
```java
	// 4-4: 가격 변경. @Transactional 안에서 부르면 save 없이도 UPDATE가 나감 (변경 감지)
	public void changePrice(int price) {
		this.price = price;
	}
```
**`ProductService.java`** — 필드 · 생성자를 바꾸고 `changeName()` 아래에 메서드 추가
```java
	private final ProductRepository products;
	private final PricePolicy pricePolicy; // 4-4: 가격 계산은 PricePolicy에 맡긴다

	// 생성자로 받는다 → 테스트에서 가짜 저장소 · 진짜 PricePolicy를 넣을 수 있다
	public ProductService(ProductRepository products, PricePolicy pricePolicy) {
		this.products = products;
		this.pricePolicy = pricePolicy;
	}
```
```java
	// 4-4: 상품을 찾아 PricePolicy가 계산한 가격으로 바꾼다
	@Transactional
	public ProductResponse raisePrice(Long number, int percent) {
		Product product = find(number); // 없는 번호면 ProductNotFoundException → 404
		product.changePrice(pricePolicy.raisePrice(product.getPrice(), percent));
		return ProductResponse.from(product);
	}
```
**`ProductController.java`** — `delete()` 아래에 추가
```java
	// 4-4: PUT /product/price?number=1&percent=10
	@PutMapping("/price")
	public ProductResponse raisePrice(@RequestParam Long number, @RequestParam int percent) { // 주소의 ?number=...&percent=... 값을 받음
		return service.raisePrice(number, percent);
	}
```
**`ProductServiceTest.java`** — `setUp()`을 바꾸고 맨 아래에 테스트 추가
```java
	@BeforeEach
	void setUp() {
		service = new ProductService(products, new PricePolicy()); // PricePolicy는 순수 계산이라 mock 말고 진짜를 넣음 (4-5 참고)
	}
```
```java
	// 4-4: 서비스는 PricePolicy의 계산 결과를 상품에 반영한다
	@Test
	void 가격_인상() {
		given(products.findById(1L)).willReturn(Optional.of(saved(1L, "펜", 1650, 10)));

		assertThat(service.raisePrice(1L, 15).price()).isEqualTo(1898);
	}
```
**`build.gradle`** — 4-3 규칙의 `includes`에 `PricePolicy`를 더함
```groovy
			includes = ['com.kyobo.product.ProductService', 'com.kyobo.product.PricePolicy'] // 4-4: 가격 정책도 90% 이상
```
- `PricePolicy`를 4-3에서 미리 넣지 않은 이유: 그때는 빈 클래스라 `PricePolicy: lines covered ratio is 0.00`으로 같이 실패함 (Claude Code 실측)

`.\gradlew.bat check`

#### 확인 ⑤-1: 테스트 · 규칙
- 테스트 **20개** 통과 (서비스 7 · 가격 정책 7 · 컨트롤러 3 · 저장소 3), `BUILD SUCCESSFUL`
- `setUp()`을 안 고치면 컴파일 실패:
  ```
  ProductServiceTest.java:...: error: constructor ProductService in class ProductService cannot be applied to given types;
    required: ProductRepository,PricePolicy
    found:    ProductRepository
  ```
- [ ] `check` 통과, 테스트 20개

#### 확인 ⑤-2: 서버로 확인
서버를 켜고(3-3) 터미널 2에서
```powershell
curl.exe -X POST http://localhost:8080/product -H "Content-Type: application/json" -d '{\"name\":\"pen\",\"price\":1650,\"stock\":10}'
curl.exe -X PUT "http://localhost:8080/product/price?number=1&percent=15"
curl.exe -X PUT "http://localhost:8080/product/price?number=1&percent=-5"
curl.exe -X PUT "http://localhost:8080/product/price?number=99&percent=10"
```
| 요청 | 응답 |
|---|---|
| `POST /product` | `{"number":1,"name":"pen","price":1650,"stock":10}` |
| `PUT /product/price?number=1&percent=15` | `{"number":1,"name":"pen","price":1898,"stock":10}` |
| `PUT ...percent=-5` | `{"timestamp":"...","status":500,"error":"Internal Server Error","path":"/product/price"}` |
| `PUT ...number=99...` | `{"timestamp":"...","status":404,"error":"Not Found","path":"/product/price"}` |
- [ ] 1650 → `1898`
- 확인이 끝나면 터미널 1에서 `Ctrl + C`

> `percent=-5`는 `PricePolicy`가 `IllegalArgumentException`으로 막았지만 **HTTP 상태로 바꾸는 곳이 없어서 500**임. 400으로 바꾸는 표준 오류 응답은 10_06에서 만듦

---

### 4-5. AI 테스트 초안 검토 (코드 없음)

> **목적**: AI에게 "ProductService의 raisePrice 테스트를 써 줘"라고**만** 요청해서 받은 초안을 우리 프로젝트 기준으로 검토함
>
> **수정**: 없음. **새 파일을 만들지 않음**

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProductServiceAiTest {

    @Mock ProductRepository productRepository;
    @Mock PricePolicy pricePolicy;
    @InjectMocks ProductServiceImpl productService;

    @Test
    public void testRaisePrice() {
        when(productRepository.findById(1L))
            .thenReturn(Optional.of(new Product(1L, "펜", 1650, 10)));
        ProductResponseDto result = productService.raisePrice(1L, 15);
        assertNotNull(result);
    }
}
```

#### 할 일
아래 표를 **먼저 직접 채워 보고** 펼쳐 봄. 그다음 초안을 고친 모습을 떠올려 4-4 ⑤의 `가격_인상` 테스트와 비교함 — 고친 결과가 그 테스트임

| 확인할 것 | 초안 | 우리 프로젝트 |
|---|---|---|
| JUnit 버전 | | |
| 서비스 클래스 이름 | | |
| 응답 타입 | | |
| `Product` 생성 | | |
| `PricePolicy`를 mock으로 둔 결과 | | |
| 단언 | | |

<details>
<summary>정답 보기</summary>

| 확인할 것 | 초안 | 우리 프로젝트 |
|---|---|---|
| JUnit 버전 | JUnit 4 (`org.junit.Test`, `@RunWith`) | JUnit 5 (`org.junit.jupiter`, Mockito는 `@ExtendWith(MockitoExtension.class)`) |
| 서비스 클래스 이름 | `ProductServiceImpl` (교재) | `ProductService` |
| 응답 타입 | `ProductResponseDto` (교재) | `ProductDtos.ProductResponse` |
| `Product` 생성 | `new Product(1L, ...)` — 없는 생성자 | `saved(1L, "펜", 1650, 10)` 도우미 |
| `PricePolicy`를 mock으로 둔 결과 | 대답을 정하지 않아 `raisePrice`가 **0**을 돌려줌 → 가격 0원 | 진짜 `new PricePolicy()`를 넣음 (순수 계산) |
| 단언 | `assertNotNull` — 0원이어도 통과 | `price()`가 `1898`인지 비교 |

원본 실측: 이름만 맞춰 돌리면 `@Mock PricePolicy` 때문에 결과 가격이 `0`인데 `assertNotNull`이라 **통과**함.

</details>

- [ ] 표를 채웠고, 초안의 단언이 4-2의 "단언 없는 테스트"와 같은 문제라는 것을 설명할 수 있음

> AI에게는 "JUnit 5, Mockito, Spring Boot 3.3, 클래스 이름 `ProductService`, 생성자 `(ProductRepository, PricePolicy)`, 1650원 15% → 1898원"처럼 **버전 · 이름 · 기대값**을 같이 줌. 받은 초안은 위 표로 검토함

---

### 4-6. (엑스트라) push하면 테스트가 돈다 — GitHub Actions CI

> **목적**: 지금까지 `check`는 내 컴퓨터에서만 돌았음. GitHub에 push할 때마다 깨끗한 리눅스 컴퓨터(러너)가 같은 검사를 돌리게 함
>
> **수정**: 저장소 루트에 `.github/workflows/day02-ci.yml` 새로 만듦. 시간이 남으면 함

원본은 `backend-final` 하나를 새 저장소로 올리지만, 이 저장소는 `day02/backend`가 **폴더 안에** 있음. 그래서 워크플로를 **저장소 루트**에 두고 작업 폴더를 지정함.

#### 할 일 ①
저장소 루트에 `.github/workflows/day02-ci.yml`
```yaml
# 4-6: push · PR마다 day02 테스트 + 커버리지 규칙(./gradlew check)을 GitHub 러너에서 돌린다
name: day02-ci

on:
  push:
    branches: [main]
    paths: ['day02/**']        # day02가 바뀔 때만 돈다
  pull_request:
    paths: ['day02/**']

defaults:
  run:
    working-directory: day02/backend   # run 명령을 이 폴더에서 실행

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v6 # 의존성 캐시
      - run: ./gradlew check                  # 테스트 + 4-3 커버리지 규칙
      - if: failure()                          # 실패했을 때만 테스트 보고서를 남긴다
        uses: actions/upload-artifact@v7
        with:
          name: test-report
          path: day02/backend/build/reports/tests/test/   # uses 단계는 working-directory가 안 먹어서 루트 기준 경로
```
- `gradlew`는 실행 권한이 있어야 리눅스에서 돎. 이 저장소에는 Claude Code가 커밋할 때 `git update-index --chmod=+x day02/backend/gradlew`로 기록해 둠
- 커밋 · push는 직접 함

#### 확인 ①
GitHub 저장소 **Actions** 탭에 초록 체크. `Run ./gradlew check` 로그 끝에 `BUILD SUCCESSFUL`

#### 할 일 ② → 확인 ②
`PricePolicyTest`의 `isEqualTo(1100)`을 `isEqualTo(1000)`으로 바꿔 commit · push → **빨간 X**. 로그에
```
PricePolicyTest > 천원을_10퍼센트_올리면_1100원() FAILED
20 tests completed, 1 failed
```
실행 화면 아래 **Artifacts**에 `test-report`가 생김

#### 할 일 ③
`1100`으로 되돌리고 push → 다시 초록
- [ ] 초록 → 빨강 → 초록을 Actions 탭에서 봄

> 4-4의 빨강 → 초록을 이제 팀 전체가 봄. 깨진 코드를 병합하지 못하게 막는 보호 브랜치, JAR 배포(CD)는 12장에서 함

---

## 5. 막히면
| 증상 | 원인 → 해결 |
|---|---|
| `JAVA_HOME is not set` / `java`를 못 찾음 | 터미널을 새로 열면 `$env:JAVA_HOME`이 사라짐. 3-1의 `$env:JAVA_HOME = ...` 줄을 다시 침 |
| `import org.junit...` · `org.springframework...`에 빨간 줄 | IntelliJ가 `day02/backend`를 Gradle 프로젝트로 모름 → 2-3. 터미널 `gradlew`는 정상 |
| `build\reports\jacoco`가 없음 | `finalizedBy 'jacocoTestReport'` 빠짐. `.\gradlew.bat jacocoTestReport`로 직접 만들어도 됨 |
| 보고서를 열었는데 색이 없음 | `build\reports\tests\`를 염 (테스트 결과 보고서). 커버리지는 `build\reports\jacoco\` |
| 보고서가 그대로 | 브라우저 `F5` 안 누름 |
| 4-3 규칙을 넣었는데 `check`가 그냥 통과 | `tasks.named('check') { dependsOn ... }` 빠짐. 또는 `test`로 돌림 (`test`는 규칙을 안 봄) |
| 4-3 `PricePolicy: lines covered ratio is 0.00` | `includes`에 `PricePolicy`를 미리 넣음. 4-4 ⑤에서 넣음 |
| 4-2 뒤로 `changeName` 관련 테스트가 계속 실패 | `// product.changeName(name);` 주석을 안 풀었음 (4-2 할 일 ④) |
| 4-4 ① 에러가 `cannot find symbol ... class PricePolicy` | `PricePolicy.java`를 지웠거나 패키지가 다름. 빈 클래스는 `com.kyobo.product`에 있어야 함 |
| `@ParameterizedTest` · `@CsvSource`에 빨간 줄 | 빈 파일에 있던 import를 지움. `org.junit.jupiter.params.ParameterizedTest` · `org.junit.jupiter.params.provider.CsvSource` |
| 4-4 ④ 뒤에도 `[3] 1650` 실패 | `RoundingMode.HALF_UP` 대신 `HALF_EVEN`/`DOWN`을 씀, 또는 아직 `double` 계산이 남음 |
| 4-4 ⑤ `constructor ProductService ... cannot be applied` | `ProductServiceTest.setUp()`에 `new PricePolicy()`를 안 넣음 |
| 4-4 ⑤ 서버 시작 시 `required a bean of type 'com.kyobo.product.PricePolicy'` | `PricePolicy`에 `@Component` 빠짐 |
| curl `POST /product`가 `400` | JSON 따옴표를 `\"`로 안 씀, 또는 한글 이름을 보냄 (3-3) |
| curl `PUT /product/price`가 `404` (번호 1) | 서버를 재시작해서 상품이 사라짐 → `POST`부터. 또는 컨트롤러 `@PutMapping("/price")` 빠짐 |
| `Port 8080 was already in use` | 이전 서버가 켜져 있음. 그 터미널에서 `Ctrl + C` |
| 4-6 `./gradlew: Permission denied` | `gradlew` 실행 권한이 기록 안 됨 → `git update-index --chmod=+x day02/backend/gradlew` 후 커밋 · push |
| 4-6 0초 만에 실패, `workflow file issue` | yml 들여쓰기(공백 2칸, 탭 금지) 확인 |

---

## 6. 알려진 한계 (면접에서 물어볼 만한 부분)
1. **커버리지 100%여도 버그가 있을 수 있음**
   - 4-2 · 4-3에서 봤듯이 숫자는 단언을 구별 못 함. 단언의 질은 코드 리뷰로, 또는 **뮤테이션 테스트**(코드를 일부러 바꿔 테스트가 잡는지 보는 도구, 예: PIT)로 잼
2. **줄 커버리지만 봄**
   - `if`의 한쪽만 실행해도 줄은 초록일 수 있음. 분기(BRANCH) 커버리지를 같이 보면 더 엄격함
3. **잘못된 요청이 500**
   - 음수 인상률이 `IllegalArgumentException` → 500. 클라이언트 잘못이니 400이어야 함 → 10_06 표준 오류 응답
4. **인상률 상한이 없음**
   - `percent=10000`도 통과함. 아주 큰 값이면 `intValueExact()`가 `ArithmeticException`(500). 요구사항에 상한을 정하고 테스트로 먼저 적어야 함
5. **가격이 `int`**
   - 원 단위라 `int`로 충분하지만, 외화 · 소수 단위 금액이면 `BigDecimal`로 저장해야 함
6. **동시에 가격을 바꾸면**
   - 두 요청이 동시에 같은 상품 가격을 올리면 하나가 덮어쓸 수 있음(갱신 손실). 낙관적 락(`@Version`) 등은 이후 JPA 심화에서

---

## 7. 다 했으면
"가이드 02 다 했어"라고 알려 주세요. 이렇게 진행합니다.
1. 작성한 코드 리뷰
2. `.\gradlew.bat check`와 서버 curl로 완료 기준 표대로 나오는지 함께 확인
3. 통과하면 기능 단위 커밋 (push는 직접)

10_03 예고: 저장소 쿼리 메서드 · 정렬 · 페이지 · JPQL. 오늘 만든 테스트 방식으로 쿼리를 확인함

## 스스로 답해볼 질문
1. 테스트 10개가 전부 통과했는데 `changeName`이 빨강이었던 이유는?
2. 커버리지가 올랐는데 기능을 지워도 테스트가 통과한 이유는? "실행"과 "단언"으로 설명해 보기 (면접 질문)
3. `./gradlew test`와 `./gradlew check`의 차이는?
4. 커버리지 목표를 100%로 잡으면 어떤 테스트가 늘어날까
5. TDD의 빨강 · 초록 · 리팩터링에서 "가장 단순한 코드"를 먼저 쓰는 이유는?
6. `1005원 10%`는 맞았는데 `1650원 15%`는 틀린 이유는? 금액 계산에 `double`을 쓰면 안 되는 이유 (면접 질문)
7. `ProductServiceTest`에서 저장소는 mock인데 `PricePolicy`는 진짜를 넣은 이유는?
8. AI 테스트 초안이 0원인데도 통과한 이유는? AI에게 테스트를 부탁할 때 무엇을 같이 줘야 하나
