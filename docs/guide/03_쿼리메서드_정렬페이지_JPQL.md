# 가이드 03 · 쿼리 메서드 · 정렬 · 페이지 · JPQL

> **한 줄 요약**: `ProductRepository`에 **메서드 이름만 적어서** 조회 쿼리를 만들고(쿼리 메서드), 정렬 · 페이지 · JPQL · 프로젝션 · 벌크 수정을 차례로 더함. 결과는 같은 상품 8개로 테스트(`@DataJpaTest`)해서 확인하고, 마지막에 **이름 검색 + 페이지 API**(`GET /product/search`)로 묶음

## 목표
- `findBy…` · `existsBy…` · `countBy…` · `findTop3By…` 같은 메서드 이름이 **어떤 SQL로 바뀌는지** 테스트 보고서에서 직접 봄
- 결과가 여러 개일 수 있는 조회를 **단건으로 받으면 왜 터지는지** 봄 → `List` / `Optional`을 고르는 기준을 설명할 수 있음
- `Sort` · `Pageable`로 정렬 · 페이지를 붙이고, `Page`가 **쿼리를 두 번**(내용 + 개수) 보내는 것을 봄
- `@Query`(JPQL), 인터페이스 프로젝션, `@Modifying` 벌크 수정을 쓰고, 벌크 수정 뒤 **옛 값이 보이는 함정**을 직접 봄
- 주소의 `page` · `size` · `sort`를 `Pageable`로 받는 검색 API를 만들고, 응답 JSON 모양을 고정함
- (엑스트라) 검색 API에 붙는 **화면을 코딩 에이전트에게 프롬프트로 시키고**(4-7), 그 화면의 완료 기준을 **Vitest 테스트로 고정**함(4-8). 화면 코드는 직접 쓰지 않음. 연습할 것은 React가 아니라 **일 시키는 순서**

## 완료 기준
| 확인 | 오늘 시작할 때 | 오늘 끝났을 때 |
|---|---|---|
| `.\gradlew.bat test` | 테스트 20개 통과 (2일차 완성) | 테스트 **30개** 통과 (쿼리 8 · 검색 API 2 추가) |
| `.\gradlew.bat check` | 통과 | 통과 (`ProductService` 줄 커버리지 `0.94`. 4-6 엑스트라까지 하면 `1.00`, 테스트 31개) |
| 서버를 켜면 | 상품 0개 | 상품 **8개**가 들어 있음 (`DataInitializer`) |
| `GET /product/search?keyword=펜&page=1&size=2&sort=price,desc` | `500` (주소 없음) | `200` · 형광펜 1200, 펜 1000 + `"page":{"size":2,"number":1,"totalElements":5,"totalPages":3}` |
| `GET /product/search?keyword=펜&sort=prise` | `500` | `500` (없는 칸으로 정렬. 400으로 바꾸는 곳이 없음. 6장) |
| (엑스트라) `day03\frontend`에서 `npm run build` · `npm test` | 폴더 없음 | 빌드 통과 · `Tests  4 passed (4)` |
| (엑스트라) http://localhost:5173 에서 '펜' · 가격 높은순 | 화면 없음 | 1쪽 펜 2,000원 · 1,500원 → 3쪽 볼펜 800원, "총 5개 · 3페이지" |

- [ ] Claude Code가 가이드 코드를 scratchpad 복사본에 단계대로 적용해 30개 통과 · `check` 통과 · 서버 응답을 확인함. 4장의 출력 · SQL · 오류 메시지는 그때 받은 값임
- [ ] 엑스트라(4-7 · 4-8)는 원본 참고 답안 `frontend-final`을 scratchpad에서 이 백엔드에 붙여 화면 · 테스트 · 망가뜨리기 결과를 확인함

## 만들 API
| 요청 | 설명 |
|---|---|
| `GET /product/search?keyword=펜&page=1&size=2&sort=price,desc` | 이름에 `keyword`가 들어간 상품을 페이지로. `page`는 0부터. 안 주면 0페이지 · 5개 · 번호순 |
| 1 · 2일차 API | `GET /product?number=1` · `POST /product` · `PUT /product` · `DELETE /product?number=1` · `PUT /product/price` 그대로 |

응답 예시 (서버를 켜면 들어 있는 8개 기준):
```json
// GET /product/search?keyword=펜&page=1&size=2&sort=price,desc
{"content":[{"number":5,"name":"형광펜","price":1200,"stock":12},{"number":1,"name":"펜","price":1000,"stock":10}],
 "page":{"size":2,"number":1,"totalElements":5,"totalPages":3}}
```

---

## 1. 개념 먼저

### 1-1. 쿼리 메서드: 메서드 이름이 곧 쿼리
- 비유: 도서관 사서에게 **정해진 양식**으로 쪽지를 주면 사서가 알아서 책을 찾아 옴. "제목이 ○○인 책 / 가격이 낮은 순 3권"
- 뜻: 저장소 인터페이스에 `findByName(String name)`처럼 **약속된 단어로 이름을 지으면** 스프링 데이터 JPA가 이름을 쪼개 읽고 쿼리를 만들어 줌. 구현 코드는 쓰지 않음
- 이름은 세 조각: **주제**(무엇을 할지) + `By` + **조건**(어떤 행을)

| 조각 | 예 | 바뀌는 SQL |
|---|---|---|
| 주제 `find…By` | `findByName` | `select … where name = ?` |
| 주제 `exists…By` | `existsByName` | `select number … fetch first ? rows only` (있나만 봄) |
| 주제 `count…By` | `countByName` | `select count(number) …` |
| 주제 `findFirst…` · `findTop3…` | `findTop3ByOrderByPriceDesc` | `… order by price desc fetch first ? rows only` |
| 조건 `Not` · `And` · `LessThan` · `Between` · `Containing` | `findByPriceBetween` | `where price between ? and ?` |
| 정렬 `OrderBy…Asc/Desc` | `…OrderByPriceAsc` | `order by price` |

- 이름 속 칸 이름(`Name`, `Price`)은 **테이블 컬럼이 아니라 `Product`의 필드 이름**. 철자가 틀리면 서버 · 테스트가 **시작할 때** 바로 실패함 (4-1에서 일부러 틀려 봄)
- 오늘 코드에서: `ProductRepository`에 4-1 ~ 4-4 메서드를 차례로 더함

### 1-2. 단건이냐 목록이냐
- 돌려받는 타입이 `List<Product>`면 0개 이상 무엇이든 받음
- `Product` · `Optional<Product>`(단건)면 "**많아야 하나**"라는 약속. 2개 이상 나오면 `IncorrectResultSizeDataAccessException` (4-2에서 봄)
- 기준: 결과가 여러 개일 **수 있으면** `List`. 단건은 `First` · `Top1` · 유일한 칸(번호 등)으로 찾을 때만. 없을 수도 있으면 `null` 대신 `Optional`

### 1-3. 정렬과 페이지
- `Sort`: 정렬 기준. `Sort.by(Sort.Order.desc("price"), Sort.Order.asc("name"))` = 가격 내림차순, 같으면 이름 오름차순
- `Pageable`: "몇 번째 페이지를 몇 개씩, 어떤 순서로". `PageRequest.of(1, 2, Sort.by("price"))`. **페이지 번호는 0부터**
- `Page<T>`: 내용 + 전체 개수 · 전체 페이지 수. 전체 개수를 알려고 **count 쿼리를 한 번 더** 보냄
- `Slice<T>`: "다음 페이지가 있나"만 알면 될 때. count 쿼리가 없음 (오늘은 안 씀)

### 1-4. JPQL · 프로젝션 · 벌크 수정
| 기능 | 언제 | 오늘 코드 |
|---|---|---|
| JPQL (`@Query`) | 메서드 이름으로 쓰기엔 길거나 복잡할 때. SQL과 비슷하지만 **엔티티 · 필드 이름**으로 씀 | `findExpensive(int min)` |
| 인터페이스 프로젝션 | 칸 몇 개만 필요할 때. 돌려받는 타입만 바꾸면 select에 그 칸만 들어감 | `ProductSummary` · `findSummaryByNameContaining` |
| 벌크 수정 (`@Modifying`) | 여러 행을 update 한 번으로 바꿀 때 | `raiseAll(name, amount)` |

### 1-5. 영속성 컨텍스트와 벌크 수정의 함정
- 비유: JPA는 한 번 읽은 상품을 **책상 위(영속성 컨텍스트)** 에 올려 둠. 같은 번호를 다시 찾으면 창고(DB)에 안 가고 책상 위 것을 줌
- 벌크 update는 **창고(DB)만** 바꿈. 책상 위의 옛 상품은 그대로 → 다시 `findById`하면 **옛 값**이 나옴 (4-4 ③에서 실패로 봄)
- `@Modifying(clearAutomatically = true)`: update 뒤에 책상을 비움 → 다음 조회는 DB에서 새로 읽음

### 1-6. 용어 정리
| 용어 | 쉬운 뜻 |
|---|---|
| 쿼리 메서드 | 이름만 약속대로 지으면 스프링 데이터가 쿼리를 만들어 주는 저장소 메서드 |
| 주제 키워드 | `find` · `exists` · `count` · `First` · `Top` — 무엇을 돌려줄지 |
| 조건 키워드 | `Not` · `And` · `LessThan` · `Between` · `Containing` — 어떤 행을 고를지 |
| `@DataJpaTest` | JPA와 H2 메모리 DB만 띄우는 테스트. 테스트마다 끝나면 롤백 |
| `Sort` / `Pageable` / `Page` | 정렬 기준 / 페이지 요청(번호 · 크기 · 정렬) / 페이지 결과(내용 + 전체 개수) |
| JPQL | 엔티티 이름(`Product`) · 필드 이름(`p.price`)으로 쓰는 JPA용 쿼리 언어 |
| 프로젝션 | 엔티티 전체가 아니라 필요한 칸만 받는 것 |
| 영속성 컨텍스트 | JPA가 읽은 엔티티를 잠시 들고 있는 곳. 같은 번호는 DB에 다시 안 감 |
| 벌크 수정 | update 한 번으로 여러 행을 바꾸는 것. 영속성 컨텍스트를 거치지 않음 |
| `CommandLineRunner` | 스프링이 다 뜬 뒤 `run(...)`을 한 번 불러 주는 인터페이스 (시작 데이터 넣기) |

### 1-7. (엑스트라) 코딩 에이전트에게 일 시키기
- 비유: 처음 온 외주 개발자. 일을 맡길 때 "화면 만들어 줘" 한마디로 끝내지 않음. **자료를 읽게 하고 → 계획을 받고 → 결과를 증거로 받고 → 틀리면 증거를 주고 고치게** 함
- 뜻: 코딩 에이전트는 파일을 읽고 명령을 실행하는 AI 도구(Claude Code · Codex · Copilot 에이전트 모드 · Cursor 등). 똑똑하지만 **기억으로 추측**하기도 함 (예: 옛 Spring 버전의 `Page` JSON 모양)
- 오늘 순서

```
① 읽게 한다 → ② 계획을 받는다 → ③ 스스로 확인하게 한다 → ④ 증거를 주고 고치게 한다   (4-7 화면)
① 테스트 계획 → ② 쓰고 돌리기 → ③ 일부러 망가뜨려 잡는지 보기 → ④ 테스트 리뷰          (4-8 테스트)
```

| 용어 | 쉬운 뜻 | 오늘 어디에 |
|---|---|---|
| 완료 기준 | "이렇게 나오면 끝"을 숫자로 적은 것. 오늘의 8개 데이터로 정답을 미리 앎 | 4-7 프롬프트 ② → 4-8에서 그대로 테스트가 됨 |
| Vite 프록시 | 화면 서버(5173)가 `/product/...` 요청을 백엔드(8080)로 대신 전달. 브라우저 입장에서는 같은 출처라 CORS 문제가 없음 | `vite.config.ts` |
| CORS | 브라우저가 **다른 출처**(5173 → 8080)로 직접 요청하면 서버 허락 없이는 막는 규칙 | 4-7 "이런 장면" 표. 자세히는 10_08 |
| Vitest · jsdom | 화면용 테스트 도구 · 브라우저 없이 화면을 그리는 가짜 브라우저 | 4-8 |
| `vi.stubGlobal('fetch', …)` | 진짜 `fetch` 대신 가짜를 넣음. 백엔드의 Mockito `given(...).willReturn(...)`과 같은 일 | 4-8 |

---

## 2. 미리 바꿔둔 설정

### 2-1. 2일차에서 가져온 파일
`day02/backend`의 **내 완성 코드**를 `day03/backend`로 복사해 둠 (빌드 결과 `build/` 제외).

| 파일 (패키지) | 상태 | 오늘 할 일 |
|---|---|---|
| `ProductRepository` (`repository`) | 2일차 그대로 + import 미리 추가 | 4-1 ~ 4-4에서 메서드 추가 |
| `ProductService` (`service`) | 2일차 그대로 + import 미리 추가 | `search()` 추가 (4-5) |
| `ProductController` (`controller`) | 2일차 그대로 + import 미리 추가 | `search()` 추가 (4-5) |
| `ProductApplication` (맨 위) | 2일차 그대로 + import 미리 추가 | 어노테이션 한 줄 추가 (4-5 ②) |
| `Product` · `ProductDtos` · `ProductNotFoundException` · `PricePolicy` | 2일차 그대로 | **안 고침** |
| 테스트 4개 (`ProductServiceTest` 7 · `PricePolicyTest` 7 · `ProductControllerTest` 3 · `ProductRepositoryTest` 3) | 2일차 그대로 | **안 고침** (4-6 엑스트라만 `ProductServiceTest`에 1개) |
| `DataInitializer` (`config`, 새 패키지) | **빈 클래스** | 4-0에서 씀 |
| `ProductSummary` (`dto`) | **빈 인터페이스** | 4-4 ②에서 씀 |
| `ProductQueryTest` (테스트 `repository`) | **빈 클래스** | 4-0 ~ 4-4에서 씀 |
| `ProductSearchApiTest` (테스트 `controller`) | **빈 클래스** | 4-5 ③에서 씀 |

- 고칠 클래스의 **역할 주석 첫 줄**에 `→ 3일차 · 03에서 … 추가`를 미리 적어 둠
- 필요한 import는 미리 넣어 둠. **4장 코드에 import가 없으면 이미 있는 것** (안 쓰는 동안 IntelliJ가 회색으로 보여도 지우지 않음)

### 2-2. 패키지
2일차 패키지 그대로에 하나를 더함.

| 패키지 (`com.kyobo.product.` 아래) | 새로 들어간 것 | 하는 일 |
|---|---|---|
| `config` (새로 만듦) | `DataInitializer` | 서버 시작 때 상품 8개 넣기 |
| `dto` | `ProductSummary` | 이름 · 가격만 담는 프로젝션 |
| (테스트) `repository` | `ProductQueryTest` | 쿼리 메서드 결과 확인 |
| (테스트) `controller` | `ProductSearchApiTest` | 검색 API의 주소 값 → `Pageable` 확인 |

- 원본(`10_03/backend-start`)은 한 폴더에 다 있고 `DataInitializer` · `ProductQueryTest`의 뼈대가 미리 채워져 있음. 이 저장소는 둘 다 빈 클래스로 두고 4-0에서 직접 씀

### 2-3. 설정 파일
- `settings.gradle`: 프로젝트 이름 `product-coverage` → `product-jpa` (원본과 같게)
- `application.properties`: `spring.application.name=product-jpa`로만 바꿈. `spring.jpa.show-sql=true`는 2일차부터 있음 → 실행된 SQL이 출력에 찍힘 (오늘의 주요 확인 도구)
- `build.gradle`: **안 바꿈.** 2일차 JaCoCo 규칙(`ProductService` · `PricePolicy` 줄 90% 이상)이 그대로 걸려 있음

### 2-4. IntelliJ
- `day03/backend/build.gradle`을 Gradle 프로젝트로 새로 연결함 → [README "IntelliJ에서 Gradle 프로젝트 연결하기"](README.md#intellij에서-gradle-프로젝트-연결하기)
- `day02`와 `day03`에 같은 이름의 클래스가 있음. 파일을 열 때 경로가 `day03`인지 확인함

### 2-5. (엑스트라) 프런트엔드 준비
- `day03/frontend` 폴더는 **만들어 두지 않음.** 에이전트가 4-7에서 만드는 것이 실습임
- 저장소 루트 `.gitignore`에 `node_modules/` · `dist/`를 더해 둠 → 설치 폴더 · 빌드 결과는 커밋에 안 들어감 (`package-lock.json`은 들어감)
- 이 PC의 Node: `node -v` → `v24.21.0`, `npm -v` → `11.19.0` (Claude Code 확인)
- 참고 답안: 원본 `C:\Users\user\Desktop\실습\10_03\frontend-final` (`src/App.tsx` 화면 하나 + `src/App.test.tsx` 테스트 4개). 저장소에는 넣지 않음. 다 끝난 뒤 비교용

---

## 3. 실행 방법과 확인 도구
4장을 시작하기 전에 한 번 읽어 둠. 오늘의 확인은 **테스트 결과 + 테스트 보고서의 SQL**, 마지막에 서버 + 브라우저.

### 3-1. 테스트 돌리기
```powershell
# 저장소 루트에서
cd day03\backend
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.12"
.\gradlew.bat test --tests '*ProductQueryTest'   # 4-0 ~ 4-4: 쿼리 테스트만
.\gradlew.bat test                               # 전체
.\gradlew.bat check                              # 전체 + 커버리지 규칙
```
- `--tests '*ProductQueryTest'`: 이름이 `ProductQueryTest`로 끝나는 테스트 클래스만 돌림. 빠름
- IntelliJ에서 `ProductQueryTest` 옆 ▶로 돌려도 됨. SQL은 아래쪽 Run 창에 바로 찍힘

### 3-2. 실행된 SQL 보는 곳
| 어디서 | 보는 법 |
|---|---|
| 테스트 보고서 | `build\reports\tests\test\index.html` → **Classes** 탭 → `com.kyobo.product.repository.ProductQueryTest` → **Standard output** 탭 |
| IntelliJ ▶ | Run 창에 `Hibernate: select ...` 줄 |
| 서버 (4-5) | `bootRun` 터미널에 `Hibernate: select ...` 줄 |

- 보고서는 **클래스 하나의 출력을 모아서** 보여 줌. 테스트마다 나뉘지 않음 → 이번 단계에서 새로 생긴 줄을 찾음
- 매 테스트 앞에 `insert into product ...`가 8줄씩 나옴 (setUp). 그 사이의 `select` · `update` 줄을 봄
- 테스트를 다시 돌린 뒤에는 브라우저에서 `F5`

### 3-3. 서버 켜고 브라우저 (4-5에서만)
```powershell
# 터미널 1 — 서버
cd day03\backend
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.12"
.\gradlew.bat bootRun
```
- 서버가 뜬 신호: `Started ProductApplication`. 바로 위에 `insert into product ...` 8줄(`DataInitializer`)
- 확인은 **브라우저 주소창**에 `http://localhost:8080/product/search?keyword=펜` 처럼 넣음. 한글을 브라우저가 알아서 인코딩해 줌
- 터미널로 하려면 한글을 인코딩한 주소를 씀 (`펜` = `%ED%8E%9C`). Claude Code는 이렇게 확인함
  ```powershell
  curl.exe "http://localhost:8080/product/search?keyword=%ED%8E%9C&page=1&size=2&sort=price,desc"
  ```
- 서버를 다시 켜면 처음 8개로 돌아감 (메모리 DB + `DataInitializer`). 끌 때는 `Ctrl + C`

### 3-4. (엑스트라) 화면 켜기 · 테스트 (4-7 · 4-8에서만)
에이전트가 `day03/frontend`를 만든 뒤에 씀
```powershell
# 터미널 1 — 백엔드 (3-3과 같음. 4-7에서만 필요, 4-8 테스트는 필요 없음)
# 터미널 2 — 화면
cd day03\frontend
npm install       # 처음 한 번 (에이전트가 이미 했으면 생략)
npm run dev       # 화면 http://localhost:5173
npm run build     # 타입 검사 + 빌드
npm test          # 4-8 Vitest
```
- 브라우저에서 `F12` → **Network** 탭에서 `search?keyword=...` 요청을 누르면 서버가 준 JSON(Response)이 보임. **Console** 탭에는 오류가 찍힘

---

## 4. 단계별 진행
- 작업 폴더: `day03/backend`
- 단계마다 맨 위에 **목적**(왜)과 **수정**(어느 파일의 어디를)을 적었음. 먼저 읽고 코드를 씀
- 저장소 메서드는 `ProductRepository`의 **맨 아래(`findByName` 아래)** 에, 테스트는 `ProductQueryTest`의 **맨 아래**에 차례로 더함
- 결과가 다르면 다음 단계로 가지 말고 5장 "막히면"을 봄

```
day03/backend/
├── settings.gradle · application.properties          (이름만 product-jpa로 바꿔 둠)
└── src/
    ├── main/java/com/kyobo/product/
    │   ├── ProductApplication.java                     (4-5 ② 어노테이션 한 줄)
    │   ├── config/DataInitializer.java                 (빈 클래스 → 4-0)
    │   ├── controller/ProductController.java           (4-5 ① search 추가)
    │   ├── service/ProductService.java                 (4-5 ① search 추가)
    │   ├── service/PricePolicy.java                    (그대로)
    │   ├── repository/ProductRepository.java           (4-1 ~ 4-4 메서드 추가)
    │   ├── entity/Product.java                         (그대로)
    │   ├── dto/ProductDtos.java                        (그대로)
    │   ├── dto/ProductSummary.java                     (빈 인터페이스 → 4-4 ②)
    │   └── exception/ProductNotFoundException.java     (그대로)
    └── test/java/com/kyobo/product/
        ├── controller/ProductControllerTest.java       (그대로)
        ├── controller/ProductSearchApiTest.java        (빈 클래스 → 4-5 ③)
        ├── repository/ProductRepositoryTest.java       (그대로)
        ├── repository/ProductQueryTest.java            (빈 클래스 → 4-0 ~ 4-4)
        ├── service/ProductServiceTest.java             (그대로. 4-6 엑스트라만)
        └── service/PricePolicyTest.java                (그대로)

day03/frontend/                                         (지금은 없음 → 4-7에서 에이전트가 만듦, 4-8에서 테스트 추가)
```

| 단계 | 할 일 | 보게 될 것 |
|---|---|---|
| 4-0 | 시작 데이터 · 테스트 뼈대 | 테스트 21개 통과, `insert` 8줄 |
| 4-1 | 주제 키워드 | `exists` · `count` · `fetch first` SQL / 철자 틀리면 저장소 테스트 5개가 한꺼번에 실패 |
| 4-2 | 조건 키워드 · 단건의 함정 | `like` · `between` SQL / 단건으로 받으면 `5 results were returned` |
| 4-3 | 정렬 · 페이지 | SQL 두 번 (내용 + count) / `No property 'prise'` |
| 4-4 | JPQL · 프로젝션 · 벌크 수정 | `select name, price`만 / `expected: 1100 but was: 1000` → 고치면 통과 |
| 4-5 | 검색 API | 경고 로그 → 한 줄로 JSON 모양 고정 → 테스트 30개 |
| 4-6 | (엑스트라) 서비스 검색 테스트 | 커버리지 `0.94` → `1.00` |
| 4-7 | (엑스트라) 검색 화면 — 에이전트에게 시키기 | API 정리 → 계획 → 프록시로 받은 JSON → 화면에서 3쪽 볼펜 800원 |
| 4-8 | (엑스트라) 화면 테스트 — Vitest | 4개 통과 → 일부러 망가뜨리면 2개 실패 → 되돌려 4개 통과 |

---

### 4-0. 시작 데이터 · 테스트 뼈대

> **목적**: 오늘 모든 확인에 쓸 **같은 상품 8개**를 서버(`DataInitializer`)와 테스트(`ProductQueryTest.setUp`)에 넣음
>
> **수정**: `config/DataInitializer.java` · `test/.../repository/ProductQueryTest.java` — 빈 클래스 선언을 아래로 바꿈

오늘의 데이터 (번호는 넣은 순서)

| 번호 | 이름 | 가격 | 재고 |
|---:|---|---:|---:|
| 1 | 펜 | 1000 | 10 |
| 2 | 펜 | 1500 | 5 |
| 3 | 펜 | 2000 | 0 |
| 4 | 볼펜 | 800 | 30 |
| 5 | 형광펜 | 1200 | 12 |
| 6 | 공책 | 3000 | 7 |
| 7 | 연필 | 500 | 50 |
| 8 | 필통 | 5000 | 2 |

#### 할 일
**`DataInitializer.java`** — `public class DataInitializer { }`를 아래로 바꿈
```java
@Component // 빈으로 등록 → 스프링이 서버 시작 때 run(...)을 불러 줌. 빠뜨리면 아무 일도 안 일어남(오류도 없음)
public class DataInitializer implements CommandLineRunner { // CommandLineRunner: 빈이 다 만들어진 뒤 run(...)이 한 번 불림

	private final ProductRepository products;

	public DataInitializer(ProductRepository products) {
		this.products = products;
	}

	@Override
	public void run(String... args) {
		products.saveAll(List.of( // 8개를 한 번에 저장. 번호는 넣은 순서대로 1~8
				new Product("펜", 1000, 10),
				new Product("펜", 1500, 5),
				new Product("펜", 2000, 0),
				new Product("볼펜", 800, 30),
				new Product("형광펜", 1200, 12),
				new Product("공책", 3000, 7),
				new Product("연필", 500, 50),
				new Product("필통", 5000, 2)));
	}
}
```
**`ProductQueryTest.java`** — `class ProductQueryTest { }`를 아래로 바꿈
```java
@DataJpaTest // JPA + H2만 띄움. 테스트마다 끝나면 롤백
class ProductQueryTest {

	@Autowired // 스프링이 만든 저장소 구현을 넣어 줌
	ProductRepository products;

	@BeforeEach // 테스트마다 먼저 실행 → 매번 같은 8개에서 시작
	void setUp() {
		products.saveAll(List.of(
				new Product("펜", 1000, 10),
				new Product("펜", 1500, 5),
				new Product("펜", 2000, 0),
				new Product("볼펜", 800, 30),
				new Product("형광펜", 1200, 12),
				new Product("공책", 3000, 7),
				new Product("연필", 500, 50),
				new Product("필통", 5000, 2)));
	}

	// 상품 목록 → 이름 목록 / 가격 목록 (단언을 짧게 쓰는 도우미)
	static List<String> names(List<Product> list) {
		return list.stream().map(Product::getName).toList();
	}

	static List<Integer> prices(List<Product> list) {
		return list.stream().map(Product::getPrice).toList();
	}

	// 4-0
	@Test
	void 이름으로_찾기() {
		assertThat(products.findByName("펜")).hasSize(3); // 이름이 정확히 "펜"인 것만 (볼펜 · 형광펜 제외)
	}
}
```
`.\gradlew.bat test`

#### 확인
```
ProductQueryTest > 이름으로_찾기() PASSED
...
BUILD SUCCESSFUL
```
- [ ] 테스트 **21개** 통과 (2일차 20 + 1)
- 보고서 Standard output (3-2):
  ```
  Hibernate: insert into product (name,price,stock,number) values (?,?,?,default)     ← 8번
  Hibernate: select p1_0.number,p1_0.name,p1_0.price,p1_0.stock from product p1_0 where p1_0.name=?
  ```
- `findByName`이 `where p1_0.name=?`가 됨. `p1_0`은 Hibernate가 붙인 테이블 별명

> `DataInitializer`는 `@DataJpaTest`에서는 **안 불림**(테스트가 `@Component`를 띄우지 않음). 그래서 2일차 `ProductRepositoryTest.각_테스트는_빈_DB에서_시작한다`도 그대로 통과함. 서버에서는 4-5에서 확인함

---

### 4-1. 주제 키워드

> **목적**: 이름 앞부분(`find` · `exists` · `count` · `First` · `Top`)만 바꿔도 **SQL이 달라지는 것**을 봄 (1-1)
>
> **수정**: `ProductRepository` 맨 아래에 메서드 4개, `ProductQueryTest` 맨 아래에 테스트 1개

#### 할 일
**`ProductRepository.java`**
```java
	// 4-1: 주제 키워드 — find · exists · count · First/Top
	Optional<Product> findFirstByNameOrderByPriceAsc(String name); // 이름이 같은 것 중 가격이 가장 싼 1개. 없으면 Optional.empty()

	boolean existsByName(String name); // 있나 없나만

	long countByName(String name); // 몇 개인가

	List<Product> findTop3ByOrderByPriceDesc(); // 가격 높은 순 3개
```
**`ProductQueryTest.java`**
```java
	// 4-1
	@Test
	void 주제_키워드() {
		assertThat(products.existsByName("지우개")).isFalse(); // 없는 이름 → false
		assertThat(products.countByName("펜")).isEqualTo(3);
		assertThat(products.findFirstByNameOrderByPriceAsc("펜")).get() // Optional 안의 값을 꺼내 검사
				.extracting(Product::getPrice).isEqualTo(1000);         // 펜 3개 중 가장 싼 것
		assertThat(prices(products.findTop3ByOrderByPriceDesc())).containsExactly(5000, 3000, 2000); // 순서까지 같아야 통과
	}
```
`.\gradlew.bat test --tests '*ProductQueryTest'`

#### 확인
- [ ] `주제_키워드() PASSED`
- 보고서에서 새로 생긴 SQL:

| 메서드 | SQL |
|---|---|
| `existsByName` | `select p1_0.number from product p1_0 where p1_0.name=? fetch first ? rows only` — **번호 하나만**, 1행만 읽고 멈춤 |
| `countByName` | `select count(p1_0.number) from product p1_0 where p1_0.name=?` |
| `findFirstByNameOrderByPriceAsc` | `... where p1_0.name=? order by p1_0.price fetch first ? rows only` |
| `findTop3ByOrderByPriceDesc` | `... order by p1_0.price desc fetch first ? rows only` |

#### 해 보기: 이름 철자를 틀림
`ProductRepository`에 `long countByNmae(String name);`(Name 오타)을 잠깐 더하고 **전체** `.\gradlew.bat test`
```
ProductQueryTest > 이름으로_찾기() FAILED
ProductQueryTest > 주제_키워드() FAILED
ProductRepositoryTest > 저장하면_번호가_붙고_다시_찾을_수_있다() FAILED
ProductRepositoryTest > 이름으로_찾기() FAILED
ProductRepositoryTest > 각_테스트는_빈_DB에서_시작한다() FAILED
22 tests completed, 5 failed
```
보고서의 실패 메시지 맨 아래 원인:
```
No property 'nmae' found for type 'Product'; Did you mean 'name'
```
- 그 메서드를 부르지도 않았는데 **저장소를 쓰는 테스트(`@DataJpaTest`) 5개가 전부** 실패함. 스프링은 시작할 때 저장소의 모든 메서드 이름을 검사함
- 컨트롤러 · 서비스 테스트는 저장소를 안 띄우거나 가짜를 써서 통과함
- [ ] 확인했으면 오타 메서드를 **지움**

---

### 4-2. 조건 키워드 — 그리고 단건의 함정

> **목적**: `By` 뒤의 조건 단어가 `where` 절로 바뀌는 것을 보고, 결과가 여러 개인데 **단건으로 받으면** 어떻게 되는지 봄 (1-2)
>
> **수정**: `ProductRepository`에 메서드 4개, `ProductQueryTest`에 테스트 1개 (+ 잠깐 해 보고 지우는 메서드 1개)

#### 할 일 ①
**`ProductRepository.java`**
```java
	// 4-2: 조건 키워드 — 결과가 여러 개일 수 있으면 List로 받는다
	List<Product> findByNameNot(String name); // name <> ?

	List<Product> findByNameAndPriceLessThan(String name, Integer price); // name = ? and price < ? (인수 순서 = 이름 속 칸 순서)

	List<Product> findByPriceBetween(Integer low, Integer high); // price between ? and ? (양 끝 포함)

	List<Product> findByNameContaining(String keyword); // name like '%keyword%'
```
**`ProductQueryTest.java`**
```java
	// 4-2
	@Test
	void 조건_키워드() {
		assertThat(products.findByNameNot("펜")).hasSize(5);
		assertThat(prices(products.findByNameAndPriceLessThan("펜", 1500))).containsExactly(1000); // 1500은 "미만"이라 빠짐
		assertThat(prices(products.findByPriceBetween(1000, 2000))).containsExactlyInAnyOrder(1000, 1500, 2000, 1200); // 순서는 상관없이 이 4개
		assertThat(names(products.findByNameContaining("펜"))).containsExactlyInAnyOrder("펜", "펜", "펜", "볼펜", "형광펜");
	}
```
`.\gradlew.bat test --tests '*ProductQueryTest'`

#### 확인 ①
- [ ] `조건_키워드() PASSED`

| 메서드 | SQL의 `where` |
|---|---|
| `findByNameNot` | `where p1_0.name<>?` |
| `findByNameAndPriceLessThan` | `where p1_0.name=? and p1_0.price<?` |
| `findByPriceBetween` | `where p1_0.price between ? and ?` — 양 끝(1000, 2000) 포함 |
| `findByNameContaining` | `where p1_0.name like ? escape '\'` — 스프링이 `?`에 `%펜%`을 넣음 |

#### 할 일 ②: 단건으로 받아 봄
교재 원본 예제 8.7과 같은 모양으로 **단건**을 받는 메서드를 잠깐 더함

**`ProductRepository.java`**
```java
	Product findByNameIsNot(String name); // 교재 원본과 같은 모양 — 확인 후 지움
```
**`ProductQueryTest.java`**
```java
	@Test
	void 단건_함정() {
		products.findByNameIsNot("펜"); // 펜이 아닌 상품은 5개
	}
```
`.\gradlew.bat test --tests '*ProductQueryTest'`

#### 확인 ②
```
ProductQueryTest > 단건_함정() FAILED
    org.springframework.dao.IncorrectResultSizeDataAccessException at ProductQueryTest.java:...
        Caused by: org.hibernate.NonUniqueResultException at ProductQueryTest.java:...
```
보고서 메시지:
```
IncorrectResultSizeDataAccessException: Query did not return a unique result: 5 results were returned
```
- `IsNot`은 `Not`과 같은 뜻. SQL은 같고 **돌려받는 타입만** 다름. 결과 5개를 상품 1개에 담을 수 없어서 실패
- [ ] 확인했으면 `findByNameIsNot` 메서드와 `단건_함정` 테스트를 **둘 다 지움**

> 교재 원본은 `Integer` 가격 칸을 `Long` 인수로 비교함(`findByPriceGreaterThan(Long price)`). Claude Code가 이 버전(Spring Boot 3.3.13)에서 돌려 보니 오류 없이 `where p1_0.price>?`로 돌았음. 그래도 인수 타입은 **필드 타입(`Integer`)에 맞춤**

---

### 4-3. 정렬과 페이지

> **목적**: 같은 조건에 `Sort` · `Pageable` 인수만 붙여 정렬 · 페이지를 만들고, `Page`가 **쿼리를 두 번** 보내는 것을 봄 (1-3)
>
> **수정**: `ProductRepository`에 메서드 2개(4-2의 `findByNameContaining`과 이름이 같고 인수만 다름), `ProductQueryTest`에 테스트 2개

#### 할 일
**`ProductRepository.java`**
```java
	// 4-3: 같은 조건에 정렬 · 페이지 인수를 붙인 판. 이름이 같아도 인수가 다르면 다른 메서드(오버로딩)
	List<Product> findByNameContaining(String keyword, Sort sort); // 정렬 기준을 호출할 때 정함

	Page<Product> findByNameContaining(String keyword, Pageable pageable); // 몇 번째 페이지 · 몇 개씩 · 정렬을 한 번에
```
**`ProductQueryTest.java`**
```java
	// 4-3
	@Test
	void 정렬() {
		List<Product> sorted = products.findByNameContaining("펜",
				Sort.by(Sort.Order.desc("price"), Sort.Order.asc("name"))); // 가격 내림차순, 가격이 같으면 이름 오름차순
		assertThat(prices(sorted)).containsExactly(2000, 1500, 1200, 1000, 800);
	}

	@Test
	void 페이지는_0부터() {
		Page<Product> page = products.findByNameContaining("펜", PageRequest.of(1, 2, Sort.by("price"))); // (페이지 번호, 크기, 정렬). 번호는 0부터

		// 가격순 800, 1000 | 1200, 1500 | 2000 → 두 번째 페이지(번호 1)
		assertThat(prices(page.getContent())).containsExactly(1200, 1500); // 이 페이지의 내용
		assertThat(page.getTotalElements()).isEqualTo(5);                  // 조건에 맞는 전체 개수 (count 쿼리로 셈)
		assertThat(page.getTotalPages()).isEqualTo(3);                     // 5개 ÷ 2개씩 = 3페이지
		assertThat(page.getNumber()).isEqualTo(1);
		assertThat(page.hasNext()).isTrue();
	}
```
`.\gradlew.bat test --tests '*ProductQueryTest'`

#### 확인
- [ ] `정렬() PASSED` · `페이지는_0부터() PASSED`
- 정렬 SQL:
  ```
  select ... from product p1_0 where p1_0.name like ? escape '\' order by p1_0.price desc,p1_0.name
  ```
- 페이지 SQL은 **두 번**:
  ```
  select ... from product p1_0 where p1_0.name like ? escape '\' order by p1_0.price offset ? rows fetch first ? rows only
  select count(p1_0.number) from product p1_0 where p1_0.name like ? escape '\'
  ```
- 첫 줄은 내용(2개 건너뛰고 2개), 둘째 줄은 `getTotalElements()`용 전체 개수

> "다음 페이지가 있나"만 필요하면 `Slice<Product>`로 받음 → count 쿼리가 없음. 무한 스크롤 화면에서 씀

#### 해 보기: 없는 칸으로 정렬
`페이지는_0부터`의 `Sort.by("price")`를 `Sort.by("prise")`로 바꿔 돌림
```
ProductQueryTest > 페이지는_0부터() FAILED
    org.springframework.data.mapping.PropertyReferenceException at ProductQueryTest.java:...
```
```
PropertyReferenceException: No property 'prise' found for type 'Product'; Did you mean 'price'
```
- 정렬 칸 이름도 **필드 이름**. 메서드 이름 오타(4-1)는 시작할 때, `Sort` 오타는 **실행할 때** 터짐
- [ ] 확인했으면 `"price"`로 되돌림

---

### 4-4. JPQL · 프로젝션 · 벌크 수정

> **목적**: 메서드 이름 대신 쿼리를 직접 쓰고(JPQL), 필요한 칸만 받고(프로젝션), 여러 행을 한 번에 바꾼 뒤 **옛 값이 보이는 함정**을 봄 (1-4, 1-5)
>
> **수정**: `ProductRepository`에 메서드 3개, `ProductSummary` 채우기, `ProductQueryTest`에 테스트 3개

#### 할 일 ①: JPQL
**`ProductRepository.java`**
```java
	// 4-4 ①: JPQL — 테이블 · 컬럼이 아니라 엔티티 이름(Product)과 필드 이름(p.price)으로 쓴다
	@Query("select p from Product p where p.price >= :min order by p.price desc") // :min 자리에 @Param("min") 값이 들어감
	List<Product> findExpensive(@Param("min") int min);
```
**`ProductQueryTest.java`**
```java
	// 4-4
	@Test
	void JPQL() {
		assertThat(prices(products.findExpensive(2000))).containsExactly(5000, 3000, 2000); // 2000 "이상"이라 2000 포함
	}
```

#### 할 일 ②: 프로젝션
**`ProductSummary.java`** — 빈 인터페이스 안에
```java
public interface ProductSummary {

	String getName(); // Product.name 칸만 읽어 옴 (getter 이름 = 필드 이름)

	Integer getPrice(); // Product.price 칸만 읽어 옴
}
```
**`ProductRepository.java`**
```java
	// 4-4 ②: 필요한 칸만 — 인터페이스 프로젝션. 돌려받는 타입만 ProductSummary로 바꿈
	List<ProductSummary> findSummaryByNameContaining(String keyword);
```
**`ProductQueryTest.java`**
```java
	@Test
	void 프로젝션은_필요한_칸만() {
		List<ProductSummary> rows = products.findSummaryByNameContaining("형광");

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getName()).isEqualTo("형광펜");
		assertThat(rows.get(0).getPrice()).isEqualTo(1200);
	}
```
- 메서드 이름의 `find`와 `By` 사이(`Summary`)는 아무 단어나 됨. 돌려받는 타입이 프로젝션을 정함

#### 할 일 ③: 벌크 수정 — 먼저 `clearAutomatically` **없이**
**`ProductRepository.java`**
```java
	// 4-4 ③: 한 번에 여러 행 수정
	@Modifying // select가 아닌 쿼리(update · delete)라는 표시. 빠뜨리면 실행 때 오류
	@Query("update Product p set p.price = p.price + :amount where p.name = :name")
	int raiseAll(@Param("name") String name, @Param("amount") int amount); // 돌려주는 int = 바뀐 행 수
```
**`ProductQueryTest.java`**
```java
	@Test
	void 벌크_수정_후에는_다시_읽어야_새_값() {
		Product pen = products.findFirstByNameOrderByPriceAsc("펜").orElseThrow(); // 1000원 펜을 먼저 읽어 둠 → 영속성 컨텍스트에 들어감

		int updated = products.raiseAll("펜", 100); // DB에서 펜 3개를 한 번에 +100

		assertThat(updated).isEqualTo(3);
		assertThat(products.findById(pen.getNumber())).get().extracting(Product::getPrice).isEqualTo(1100); // 다시 읽은 값이 DB의 새 값인가
	}
```
`.\gradlew.bat test --tests '*ProductQueryTest'`

#### 확인 ①~③
```
ProductQueryTest > JPQL() PASSED
ProductQueryTest > 프로젝션은_필요한_칸만() PASSED
ProductQueryTest > 벌크_수정_후에는_다시_읽어야_새_값() FAILED
    org.opentest4j.AssertionFailedError at ProductQueryTest.java:...
8 tests completed, 1 failed
```
```
expected: 1100
 but was: 1000
```
| 기능 | 보고서의 SQL |
|---|---|
| JPQL | `select ... from product p1_0 where p1_0.price>=? order by p1_0.price desc` — `Product`·`p.price`가 `product`·`p1_0.price`로 바뀜 |
| 프로젝션 | `select p1_0.name,p1_0.price from product p1_0 where p1_0.name like ? escape '\'` — **번호 · 재고를 안 읽음** |
| 벌크 수정 | `update product p1_0 set price=(p1_0.price+cast(? as integer)) where p1_0.name=?` |
- [ ] JPQL · 프로젝션 통과, 벌크 수정은 `1100`이 아니라 `1000`으로 **실패**
- update 뒤 `findById`의 `select`가 **안 나감.** DB에 안 가고 영속성 컨텍스트의 옛 펜(1000원)을 돌려준 것 (1-5)

#### 할 일 ④: `clearAutomatically = true`
`raiseAll` 위의 `@Modifying` 줄을 바꿈
```java
	@Modifying(clearAutomatically = true) // 끝나면 영속성 컨텍스트를 비움 → 다음 조회는 DB에서 새로 읽음. 빠뜨리면 옛 값이 보임 (4-4 ③)
```
`.\gradlew.bat test --tests '*ProductQueryTest'`

#### 확인 ④
- [ ] `ProductQueryTest` **8개 모두** 통과 (`8 tests completed`가 안 나오고 `BUILD SUCCESSFUL`)

> 이 테스트는 `@DataJpaTest`가 열어 준 트랜잭션 안에서 돎. 서비스에서 `raiseAll`을 부를 때는 그 서비스 메서드에 `@Transactional`을 붙임 (수정 쿼리는 트랜잭션이 필요함)

#### 해 보기: `@Modifying`을 뺌
`@Modifying(...)` 줄을 잠깐 지우고 돌림
```
InvalidDataAccessApiUsageException: Query executed via 'getResultList()' or 'getSingleResult()' must be a 'select' query [update Product p set p.price = p.price + :amount where p.name = :name]
```
- [ ] 확인했으면 `@Modifying(clearAutomatically = true)`를 되돌림

---

### 4-5. 검색 API

> **목적**: 주소의 `page` · `size` · `sort`를 `Pageable`로 받아 4-3의 페이지 조회를 API로 내보내고, 응답 JSON 모양을 고정함
>
> **수정**: `ProductService` · `ProductController`에 `search()`, `ProductApplication`에 한 줄, `ProductSearchApiTest` 채우기

#### 할 일 ①: 서비스 · 컨트롤러
**`ProductService.java`** — `getProduct()` 아래에 추가
```java
	// 4-5: 이름 검색 + 페이지. 엔티티 Page를 응답 DTO Page로 바꿔 내보낸다
	public Page<ProductResponse> search(String keyword, Pageable pageable) {
		return products.findByNameContaining(keyword, pageable).map(ProductResponse::from); // Page.map: 내용만 바꾸고 페이지 정보(전체 개수 등)는 그대로
	}
```
**`ProductController.java`** — `get()` 아래에 추가
```java
	// 4-5: GET /product/search?keyword=펜&page=1&size=2&sort=price,desc
	@GetMapping("/search")
	public Page<ProductResponse> search(@RequestParam String keyword,
			@PageableDefault(size = 5, sort = "number") Pageable pageable) { // 주소의 page · size · sort를 Pageable로 묶어 받음. 안 오면 0페이지 · 5개 · 번호순
		return service.search(keyword, pageable);
	}
```
서버를 켜고(3-3) 브라우저 주소창에 차례로
```
http://localhost:8080/product/search?keyword=펜&page=1&size=2&sort=price,desc
http://localhost:8080/product/search?keyword=펜
```

#### 확인 ①
- 서버 시작 로그에 `insert into product ...` 8줄 (`DataInitializer`)
- 첫 주소의 응답 (한 줄로 나옴. 보기 좋게 줄을 나눔):
  ```json
  {"content":[{"number":5,"name":"형광펜","price":1200,"stock":12},{"number":1,"name":"펜","price":1000,"stock":10}],
   "pageable":{"pageNumber":1,"pageSize":2,"sort":{"empty":false,"sorted":true,"unsorted":false},"offset":2,"paged":true,"unpaged":false},
   "last":false,"totalPages":3,"totalElements":5,"size":2,"number":1,
   "sort":{"empty":false,"sorted":true,"unsorted":false},"first":false,"numberOfElements":2,"empty":false}
  ```
- 결과는 맞지만(가격 내림차순 2000, 1500 | **1200, 1000** | 800), 서버 터미널에 경고가 찍힘
  ```
  WARN ... : Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!
  ```
- 서버 SQL: `... order by p1_0.price desc offset ? rows fetch first ? rows only` + `select count(...)` (4-3과 같음)
- 두 번째 주소: 펜 · 펜 · 펜 · 볼펜 · 형광펜(번호 1~5순), `"size":5,"number":0`

> `Page` 객체(`PageImpl`)를 그대로 JSON으로 만들면 스프링 버전이 바뀔 때 모양이 달라질 수 있다는 경고. 화면(프런트엔드)이 이 모양에 기대면 깨짐

#### 할 일 ②: JSON 모양 고정
서버를 끄고(`Ctrl + C`) **`ProductApplication.java`** — `@SpringBootApplication` 아래에 한 줄
```java
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) // 4-5 ②: Page를 {content, page} 모양으로 내보냄. 빠뜨리면 PageImpl을 그대로 내보내며 경고
public class ProductApplication {
```
서버를 다시 켜고 같은 첫 주소를 엶

#### 확인 ②
```json
{"content":[{"number":5,"name":"형광펜","price":1200,"stock":12},{"number":1,"name":"펜","price":1000,"stock":10}],
 "page":{"size":2,"number":1,"totalElements":5,"totalPages":3}}
```
- [ ] `Serializing PageImpl` 경고가 **안 나옴** (`open-in-view` 경고는 시작할 때 늘 나오는 것. 오늘과 무관)
- [ ] 응답이 `content` + `page` 두 덩어리
- 확인이 끝나면 서버 `Ctrl + C`

#### 할 일 ③: API 테스트
**`ProductSearchApiTest.java`** — `class ProductSearchApiTest { }`를 아래로 바꿈
```java
@WebMvcTest(ProductController.class) // 컨트롤러만 띄움 (DB 없음)
class ProductSearchApiTest {

	@Autowired
	MockMvc mvc; // 서버를 켜지 않고 요청을 흉내 내는 도구

	@MockBean // 진짜 서비스 대신 가짜를 스프링에 넣음
	ProductService service;

	@Test
	void 검색_파라미터가_Pageable로_바뀐다() throws Exception {
		given(service.search(eq("펜"), any(Pageable.class))).willReturn(new PageImpl<>( // 가짜 서비스의 대답: 전체 5개 중 1페이지(크기 2)
				List.of(new ProductResponse(5L, "형광펜", 1200, 12)), PageRequest.of(1, 2), 5));

		mvc.perform(get("/product/search").param("keyword", "펜")
				.param("page", "1").param("size", "2").param("sort", "price,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("형광펜"))
				.andExpect(jsonPath("$.page.totalElements").value(5)) // $.page.… 는 VIA_DTO 모양일 때만 있음
				.andExpect(jsonPath("$.page.totalPages").value(3))
				.andExpect(jsonPath("$.page.number").value(1));

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class); // 서비스에 넘어간 Pageable을 붙잡을 그릇
		verify(service).search(eq("펜"), captor.capture());
		assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
		assertThat(captor.getValue().getPageSize()).isEqualTo(2);
		assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("price"))); // "price,desc" → 가격 내림차순
	}

	@Test
	void 아무것도_안_주면_기본값_size5_번호순() throws Exception {
		given(service.search(eq("펜"), any(Pageable.class))).willReturn(new PageImpl<>(List.of())); // 빈 결과

		mvc.perform(get("/product/search").param("keyword", "펜")).andExpect(status().isOk());

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(service).search(eq("펜"), captor.capture());
		assertThat(captor.getValue().getPageNumber()).isZero();                 // page 기본값 0
		assertThat(captor.getValue().getPageSize()).isEqualTo(5);               // @PageableDefault(size = 5)
		assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("number"));   // @PageableDefault(sort = "number")
	}
}
```
- `eq("펜")`: 인수 하나에 매처(`any(...)`)를 쓰면 **나머지 인수도 매처**여야 함. 그래서 `"펜"` 대신 `eq("펜")`

`.\gradlew.bat check`

#### 확인 ③
```
ProductSearchApiTest > 검색_파라미터가_Pageable로_바뀐다() PASSED
ProductSearchApiTest > 아무것도_안_주면_기본값_size5_번호순() PASSED
...
BUILD SUCCESSFUL
```
- [ ] 테스트 **30개** 통과 (서비스 7 · 가격 정책 7 · 컨트롤러 3 · 검색 API 2 · 저장소 3 · 쿼리 8), `check` 통과
- 커버리지 보고서(`build\reports\jacoco\test\html\index.html`)에서 `ProductService.search` 줄이 **빨강**. 서비스 테스트가 `search`를 안 부르기 때문(검색 API 테스트의 서비스는 가짜). 줄 커버리지는 17줄 중 16줄 = `0.94`라 90% 규칙은 통과함 → 4-6

---

### 4-6. (엑스트라) 서비스 검색 테스트 — 커버리지 되돌리기

> **목적**: 2일차에 `1.00`이던 `ProductService` 줄 커버리지가 `search` 때문에 떨어진 것을 서비스 단위 테스트로 채움. 이번에는 **단언이 있는** 테스트로 (2일차 4-2)
>
> **수정**: `ProductServiceTest` — import 4개 추가, 맨 아래에 테스트 1개

#### 할 일
**`ProductServiceTest.java`** — import에 추가 (이 파일은 2일차 파일이라 미리 넣어 두지 않음)
```java
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```
맨 아래에 추가
```java
	// 3일차 4-6: 검색 결과(엔티티 Page)가 응답 DTO Page로 바뀌고, 페이지 정보는 그대로인가
	@Test
	void 검색은_엔티티_페이지를_응답_페이지로_바꾼다() {
		Pageable pageable = PageRequest.of(1, 2);
		given(products.findByNameContaining("펜", pageable))
				.willReturn(new PageImpl<>(List.of(saved(5L, "형광펜", 1200, 12)), pageable, 5)); // 전체 5개 중 1페이지

		Page<ProductResponse> page = service.search("펜", pageable);

		assertThat(page.getContent()).containsExactly(new ProductResponse(5L, "형광펜", 1200, 12)); // 내용이 DTO로 바뀜
		assertThat(page.getTotalElements()).isEqualTo(5);                                         // 페이지 정보는 그대로
	}
```
`.\gradlew.bat check`

#### 확인
- [ ] 테스트 **31개** 통과, `check` 통과
- [ ] 커버리지 보고서에서 `ProductService` 줄 커버리지 `0.94` → **`1.00`** (`search` 줄 초록)

---

### 4-7. (엑스트라) 검색 화면 — 코딩 에이전트에게 시키기

> **목적**: 4-5의 검색 API에 붙는 화면을 **에이전트에게 프롬프트로** 만들게 함. 화면 코드는 직접 쓰지 않음. 연습할 것은 일 시키는 순서 (1-7)
>
> **수정**: 내가 고치는 파일 없음. 에이전트가 `day03/frontend`를 새로 만듦. `day03/backend`는 **한 줄도 안 바뀌어야 함**

**준비**
- 4-5까지 끝난 상태 (`VIA_DTO`까지). 백엔드 서버를 켜 둠 (3-3)
- 에이전트는 저장소 루트(`practice_SpringAdvanced`)에서 **새로** 엶. Claude Code라면 이 대화가 아니라 새 세션 → 오늘 코드를 모르는 상태에서 직접 읽게 해야 연습이 됨
- 채팅형 AI(ChatGPT 등)만 있으면 ①의 API 정리를 직접 붙여 주고, ③의 확인은 내가 함

#### 할 일 ①: 읽게 한다 — 코드는 아직 금지
```text
day03/backend의 상품 검색 API(GET /product/search)를 코드에서 찾아 읽고 정리해 줘.
- 요청 파라미터: 이름, 필수 여부, 기본값, 형식(sort 포함)
- 응답 JSON 모양: 실제 키 이름 그대로
- 근거가 된 파일 이름
아직 코드는 쓰지 마.
```

#### 확인 ①
답에 아래가 다 있어야 함 (Claude Code가 서버에 직접 보내 확인한 값)

| 항목 | 정답 | 근거 |
|---|---|---|
| `keyword` | 필수. 아예 빼면 `400`, `keyword=`(빈 값)는 `200`으로 8개 전부 (`"totalElements":8`) | `ProductController`의 `@RequestParam String keyword` |
| `page` · `size` · `sort` | `page`는 **0부터**, `size` 기본 5, `sort=price,desc` 형식, 기본은 번호순 | `@PageableDefault(size = 5, sort = "number")` |
| 응답 | `content` 배열 + `page` 안의 `size` · `number` · `totalElements` · `totalPages` | `ProductApplication`의 `VIA_DTO` |

> 에이전트가 `totalElements`가 **맨 바깥**에 있다고 답하면 옛 Spring 버전의 응답 모양을 기억으로 쓴 것. 4-5 ①에서 본 `"pageable":{...},"totalElements":5,...` 모양임. "`ProductApplication`을 다시 읽어"라고 돌려보냄. 이걸 걸러 내려고 ①을 먼저 함

#### 할 일 ②: 목표 · 제약 · 완료 기준을 주고 계획을 받는다
```text
위 API로 상품 검색 화면을 만들 거야. 먼저 계획만 보여 줘: 만들 파일 목록과 각 파일이 하는 일. 내가 승인하면 만든다.

[목표] 검색어 입력 → 결과 표(번호·이름·가격·재고), 정렬 선택(등록순·가격 낮은순·가격 높은순), 이전/다음 쪽
[위치] day03/backend 옆에 day03/frontend 폴더를 새로 만든다. Vite + React + TypeScript. 다른 라이브러리는 추가하지 않는다.
[제약] day03/backend는 한 줄도 고치지 않는다. 브라우저가 8080을 직접 부르지 말고 Vite 프록시(/product → http://localhost:8080)를 쓴다.
[완료 기준]
1. npm run build 통과
2. '펜', 가격 높은순, 한 쪽에 2개 → 1쪽 펜 2000·펜 1500, 3쪽 볼펜 800, "총 5개 · 3페이지"
3. '지우개' → "결과 없음", 이전·다음 버튼 둘 다 눌리지 않음
4. 화면의 쪽 번호는 1부터, 서버에 보내는 page는 0부터
```

#### 확인 ②: 승인 전에 거름
- [ ] 고칠 파일에 `day03/backend`의 파일이 있음 → **거절.** "제약 위반, 프록시로 해결해"
- [ ] `axios` · `react-query` · `tailwind` 같은 라이브러리가 있음 → 빼라고 함. `fetch`로 충분함
- [ ] 만들 파일이 대략 `package.json` · `vite.config.ts` · `tsconfig.json` · `index.html` · `src/main.tsx` · `src/App.tsx` 정도인가 (참고 답안의 구성)

> 네 칸 중 **완료 기준**이 가장 중요함. 오늘의 8개 데이터로 정답을 미리 알기 때문에, 에이전트가 "다 됐다"고 할 때 맞는지 바로 판정할 수 있음. 4-8에서 이 완료 기준을 그대로 테스트로 바꿈

#### 할 일 ③: 승인하고, 스스로 확인하게 한다
```text
계획대로 진행해. 끝나면 직접 확인하고 증거를 보여 줘.
- npm run build 결과 마지막 줄
- dev 서버를 띄운 뒤 프록시(5173)를 거쳐 완료 기준 2·3의 요청을 보내고 받은 JSON 그대로
- 확인하지 못한 항목은 못 했다고 따로 적어
```

#### 확인 ③
에이전트가 보여 준 JSON이 아래와 같아야 함 (Claude Code가 5173 프록시를 거쳐 받은 값)
```text
GET /product/search?keyword=펜&page=0&size=2&sort=price,desc
{"content":[{"number":3,"name":"펜","price":2000,"stock":0},{"number":2,"name":"펜","price":1500,"stock":5}],
 "page":{"size":2,"number":0,"totalElements":5,"totalPages":3}}

GET /product/search?keyword=펜&page=2&size=2&sort=price,desc
{"content":[{"number":4,"name":"볼펜","price":800,"stock":30}],"page":{"size":2,"number":2,"totalElements":5,"totalPages":3}}

GET /product/search?keyword=지우개&page=0&size=2&sort=number,asc
{"content":[],"page":{"size":2,"number":0,"totalElements":0,"totalPages":0}}
```
그다음 직접 화면을 엶 (3-4의 `npm run dev` → http://localhost:5173). 참고 답안 화면에서 Claude Code가 본 것:

| 조작 | 화면 |
|---|---|
| 처음 열기 (펜 · 등록순) | 총 5개 · 3페이지 / 1 펜 1,000원 · 2 펜 1,500원 / `이전`(비활성) 1 / 3 `다음` |
| 정렬 → 가격 높은순 | 3 펜 2,000원 · 2 펜 1,500원 / 1 / 3 |
| `다음` | 5 형광펜 1,200원 · 1 펜 1,000원 / 2 / 3 |
| `다음` | 4 볼펜 800원 / 3 / 3 (`다음` 비활성) |
| 검색어 `지우개` → 검색 | 총 0개 · 0페이지 / 결과 없음 / 0 / 0, `이전` · `다음` 둘 다 비활성 |
| 백엔드를 끄고 새로고침 | `검색 실패: HTTP 502 — 서버(8080)가 켜져 있는지 확인` (프록시가 8080에 못 닿아 502) |

- [ ] 완료 기준 2 · 3이 화면에서도 맞음. 에이전트가 만든 화면은 모양 · 문구가 달라도 됨 (가격 `2000` / `2,000원` 등). **숫자와 버튼 상태**가 맞으면 통과

> "확인 못 한 것은 못 했다고 적어" 한 줄이 근거 없는 "완료!"를 막음

#### 할 일 ④: 틀린 곳은 증거를 주고 고치게 한다
틀린 곳이 보이면 "안 돼요"라고 하지 않음. **본 것**과 **기대한 것**을 그대로 붙임 (예시)
```text
'지우개'로 검색하면 쪽 표시가 "1 / 0"이고 다음 버튼이 눌린다. 기대: 결과 없음, 버튼 둘 다 비활성.
F12 Network의 응답: {"content":[],"page":{"size":2,"number":0,"totalElements":0,"totalPages":0}}
원인을 먼저 한 줄로 말하고 고쳐. 고친 뒤 같은 요청으로 다시 확인해.
```

**이런 장면이 보이면**

| 화면에서 본 것 | 원인 | 에이전트에게 줄 말 |
|---|---|---|
| Console에 `Access to fetch at 'http://localhost:8080/...' from origin 'http://localhost:5173' has been blocked by CORS policy` (Claude Code 실측) | `fetch`가 8080을 직접 부름 | "주소를 `/product/...`로 바꾸고 프록시를 써". 백엔드에 `@CrossOrigin`을 붙이는 수정은 **거절** (CORS는 10_08) |
| 표는 나오는데 "총 undefined개" | 응답 모양을 기억으로 추측 (`page` 안이 아니라 바깥에서 찾음) | ①의 정리 결과를 다시 붙여 줌 |
| 2쪽을 누르면 3쪽 내용 | 화면 번호(1부터)를 그대로 서버에 보냄 | 완료 기준 4를 다시 붙여 줌 |
| 검색어 지우고 검색 → `400` | `keyword`를 빼고 보냄 (빈 값이면 `200`) | "빈 검색어도 `keyword=`로 보내" |

#### 마지막: 바뀐 파일을 받아 본다
```text
이번 작업에서 만들거나 바꾼 파일 목록을 보여 줘. day03/backend에서 바뀐 것이 있으면 무엇을 왜 바꿨는지도.
```
- [ ] `day03/backend`의 파일이 하나도 없음. 직접도 확인: 저장소 루트에서 `git status` → `day03/frontend/`만 새로 보여야 함 (`node_modules`는 `.gitignore`로 안 보임)
- [ ] 참고 답안 `C:\Users\user\Desktop\실습\10_03\frontend-final\src\App.tsx`와 비교해 봄. 모양은 달라도 완료 기준 1~4를 통과하면 정답

---

### 4-8. (엑스트라) 화면 테스트 — Vitest로 완료 기준 고정

> **목적**: 4-7에서 사람이 눌러서 확인한 완료 기준을 테스트로 바꿔 `npm test` 한 번으로 확인함. 백엔드에서 JUnit으로 하던 일(2일차)을 화면에서 Vitest로 함. 테스트 코드도 **에이전트에게** 시킴
>
> **수정**: 내가 고치는 파일 없음. 에이전트가 `day03/frontend`에 테스트를 더함. **백엔드 서버는 필요 없음** (가짜 `fetch`로 돎)

```
① 테스트 계획을 받는다 → ② 쓰고 돌리게 한다 → ③ 일부러 망가뜨려 테스트가 잡는지 본다 → ④ 테스트를 리뷰한다
```
③이 핵심. 에이전트가 쓴 테스트는 **항상 통과하는 테스트**일 수 있음. 코드를 망가뜨려도 통과하는 테스트는 아무것도 지켜 주지 않음 (2일차 4-2 "단언 없는 테스트"와 같은 이야기)

#### 할 일 ①: 테스트 계획을 받는다
```text
day03/frontend의 검색 화면(App.tsx)에 Vitest 테스트를 붙일 거야. 먼저 계획만 보여 줘: 설치할 패키지, 만들 파일, 테스트 이름 목록.

[도구] vitest, jsdom, @testing-library/react, @testing-library/dom 만 devDependencies로 추가한다.
[제약] 실제 서버(8080)를 부르지 않는다. fetch를 가짜로 바꿔서 아래 응답을 돌려준다.
       App.tsx는 고치지 않는다. 고쳐야 테스트할 수 있으면 이유를 먼저 말한다.
       화면에 보이는 글자·버튼으로 확인한다(컴포넌트 내부 상태를 직접 보지 않는다).
[테스트할 것]
1. 첫 화면: fetch가 page=0, size=2로 불리고 "총 5개 · 3페이지"와 가격이 보인다
2. '다음'을 누르면 서버에는 page=1, 화면에는 "2 / 3"
3. 결과가 없으면 "결과 없음", 이전·다음 둘 다 비활성
4. 서버가 꺼져 있으면(fetch 실패) 안내 문구가 보인다
[가짜 응답]
있음: {"content":[{"number":1,"name":"펜","price":1000,"stock":10},{"number":2,"name":"펜","price":1500,"stock":5}],"page":{"size":2,"number":0,"totalElements":5,"totalPages":3}}
없음: {"content":[],"page":{"size":2,"number":0,"totalElements":0,"totalPages":0}}
```
- 4-7에서 만든 화면이 첫 화면에 바로 검색하지 않는 모양이면(검색 버튼을 눌러야 시작) 테스트할 것 1의 "첫 화면"을 "펜으로 검색하면"으로 바꿔 씀

#### 확인 ①: 승인 전에 거름
- [ ] 테스트할 것 1~4가 테스트 이름 목록에 하나씩 있음
- [ ] `msw` · `@testing-library/jest-dom` · `@testing-library/user-event` 같은 패키지가 더 있음 → 지금은 빼라고 함. 네 개로 충분함
- [ ] `App.tsx`를 고친다고 하면 이유를 들음. 참고 답안은 `App.tsx`를 고치지 않고 테스트함

> [가짜 응답]은 4-7 ③에서 **실제 서버로 받은 JSON**과 같은 모양. 가짜도 진짜와 모양이 같아야 테스트가 의미 있음. 에이전트가 응답을 지어내게 두면 `totalElements`를 바깥에 두는 옛 모양을 만들 수 있음

#### 할 일 ②: 쓰고, 돌리고, 증거를 보여 준다
```text
계획대로 진행해. 끝나면 npm test 결과를 마지막 5줄 그대로 보여 줘. npm run build도 다시 통과하는지 확인해.
```
직접도 돌림
```powershell
cd day03\frontend
npm test
npm run build
```

#### 확인 ②
```text
 Test Files  1 passed (1)
      Tests  4 passed (4)
```
- [ ] 4개 통과, `npm run build` 통과 (참고 답안으로 Claude Code가 받은 결과)
- 참고 답안 테스트의 핵심 부분과 백엔드에서 비슷한 것:

```tsx
// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import App from './App'

afterEach(() => {
  cleanup()             // 앞 테스트가 그린 화면을 지움. 빠뜨리면 "Found multiple elements"
  vi.unstubAllGlobals() // 가짜 fetch를 되돌림
})

test('다음을 누르면 서버에는 page=1, 화면에는 2쪽', async () => {
  const fetch = vi.fn()
    .mockReturnValueOnce(json({ content: [/* 펜 1000 */], page: page(0, 5, 3) })) // 첫 번째 호출의 대답
    .mockReturnValueOnce(json({ content: [/* 펜 2000 */], page: page(1, 5, 3) })) // 두 번째 호출의 대답
  vi.stubGlobal('fetch', fetch) // 진짜 fetch 대신 가짜

  render(<App />)                                     // 가짜 브라우저(jsdom)에 화면을 그림
  fireEvent.click(await screen.findByText('다음'))    // 글자가 나타날 때까지 기다렸다 클릭

  expect(await screen.findByText('2 / 3')).toBeTruthy()
  expect(fetch).toHaveBeenLastCalledWith(expect.stringContaining('page=1&')) // 마지막 요청 주소에 page=1
})
```

| 코드 | 하는 일 | 백엔드에서 비슷한 것 |
|---|---|---|
| `vi.stubGlobal('fetch', ...)` | 진짜 `fetch` 대신 가짜를 넣음 | Mockito `given(...).willReturn(...)` |
| `render(<App />)` | 화면을 가짜 브라우저(jsdom)에 그림 | — |
| `findByText` | 글자가 나타날 때까지 기다렸다 찾음 (`fetch`는 비동기) | — |
| `toHaveBeenLastCalledWith` | 가짜가 어떤 인수로 불렸는지 봄 | Mockito `verify(...)` |

#### 할 일 ③: 일부러 망가뜨려서 테스트가 잡는지 본다
```text
테스트가 진짜 버그를 잡는지 확인하고 싶어.
App.tsx에서 서버로 보내는 page를 page + 1로 바꿔서(화면 번호를 그대로 보내는 버그) npm test를 돌려 결과를 보여 줘.
확인이 끝나면 원래대로 되돌리고 다시 4개 통과를 보여 줘.
```

#### 확인 ③
망가뜨린 상태에서 **2개 실패** (참고 답안으로 Claude Code가 받은 결과)
```text
 × 첫 화면은 펜을 0쪽부터 2개씩 요청하고 결과를 보여 준다
 × 다음을 누르면 서버에는 page=1, 화면에는 2쪽
      Tests  2 failed | 2 passed (4)
```
- [ ] 2개 실패 → 되돌린 뒤 `Tests  4 passed (4)`
- [ ] 마지막에 `App.tsx`가 원래대로인지 직접 열어 봄 (`page + 1`이 남지 않았나)

> 망가뜨렸는데 **전부 통과**하면 테스트가 `page` 값을 보지 않는 것. 결과를 그대로 붙여 주고 "page를 확인하는 단언이 없다, 추가해"라고 함

**해 보기** 다른 버그로도 망가뜨려 봄. 어느 테스트가 잡을지 **먼저 예상**하고 돌림 (에이전트에게 시켜도, 직접 고쳐도 됨. 끝나면 되돌림)

| 망가뜨리기 | 참고 답안에서 잡은 테스트 (Claude Code 실측) |
|---|---|
| `다음` 버튼의 `disabled` 조건을 지움 | `결과가 없으면 "결과 없음"이고 이전·다음 둘 다 비활성` 1개 실패 (`expected false to be true`) |
| 오류 안내 문구에서 `서버(8080)`를 지움 | `서버가 꺼져 있으면 안내를 보여 준다` 1개 실패 (`expected '검색 실패: Failed to fetch' to contain '서버(8080)'`) |

#### 할 일 ④: 테스트를 리뷰한다
```text
src/App.test.tsx를 리뷰해 줘. 아래 질문에 예/아니오와 근거 줄로 답해.
- 실제 서버나 localhost:8080을 부르는 곳이 있나?
- 테스트할 것 1~4가 각각 어느 테스트에 있나?
- 테스트끼리 순서를 바꿔 돌려도 결과가 같나?
- App.tsx를 바꿨나? 바꿨다면 무엇을 왜?
```

#### 확인 ④
- [ ] 에이전트의 답을 그대로 믿지 않고 파일을 열어 **근거 줄을 직접** 봄
- [ ] 참고 답안 `frontend-final\src\App.test.tsx`와 모양이 달라도 ③에서 망가뜨린 버그를 잡으면 통과

> 4-7(화면)과 4-8(테스트)을 합치면 **완료 기준 → 에이전트 구현 → 테스트로 고정** 순서가 됨. 다음부터 에이전트에게 화면을 고치게 할 때는 "고친 뒤 npm test 통과를 보여 줘"를 프롬프트에 붙임

---

## 5. 막히면
| 증상 | 원인 → 해결 |
|---|---|
| `JAVA_HOME is not set` / `java`를 못 찾음 | 터미널을 새로 열면 `$env:JAVA_HOME`이 사라짐. 3-1의 `$env:JAVA_HOME = ...` 줄을 다시 침 |
| `import org.springframework...`에 빨간 줄 | IntelliJ가 `day03/backend`를 Gradle 프로젝트로 모름 → 2-4 |
| 고친 파일이 반영 안 됨 / 테스트가 예전 그대로 | `day02`의 같은 이름 파일을 고침. 탭 위 경로가 `day03`인지 확인 |
| `@DataJpaTest` 테스트가 **전부** 실패, `No property 'xxx' found for type 'Product'` | 저장소 메서드 이름 철자. `Did you mean` 뒤의 이름으로 고침 (4-1) |
| `IncorrectResultSizeDataAccessException: ... 5 results were returned` | 여러 개 나오는 조회를 `Product`로 받음 → `List<Product>`. 4-2 ②의 메서드를 안 지움 |
| `containsExactly` 실패, 값은 같은데 순서만 다름 | 순서가 정해지지 않은 조회(`OrderBy` · `Sort` 없음)는 `containsExactlyInAnyOrder` |
| `PropertyReferenceException: No property 'prise'` | `Sort.by(...)` · 주소의 `sort=` 칸 이름 철자 (4-3) |
| 4-3 `페이지는_0부터`에서 내용이 `[800, 1000]` | `PageRequest.of(0, ...)`로 씀. 두 번째 페이지는 번호 `1` |
| `cannot find symbol: class ProductSummary` | `ProductSummary.java`를 다른 패키지에 둠. `com.kyobo.product.dto`에 있어야 함 |
| 4-4 `expected: 1100 but was: 1000` (할 일 ④ 뒤에도) | `@Modifying(clearAutomatically = true)`로 안 바꿈 |
| `InvalidDataAccessApiUsageException: ... must be a 'select' query` | `raiseAll` 위의 `@Modifying` 빠짐 |
| 저장소 테스트 전부 실패, `Using named parameters for method ... findExpensive(int) but parameter 'Optional[minimum]' not found in annotated query` | JPQL의 `:min`과 `@Param("…")` 이름이 다름 (Claude Code 실측. `@Query`도 시작할 때 검사함) |
| 서버를 켰는데 검색 결과가 `"content":[]` | `DataInitializer`에 `@Component` 빠짐 (오류 없이 데이터만 없음) |
| 4-5 ③ `No value at JSON path "$.page.totalElements"` | `ProductApplication`의 `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` 빠짐 (Claude Code 실측) |
| 4-5 ③ `InvalidUseOfMatchersException: Invalid use of argument matchers! 2 matchers expected, 1 recorded` | `given(service.search("펜", any(...)))`처럼 매처와 값을 섞음 → `eq("펜")` (Claude Code 실측) |
| 검색 주소에서 `sort=prise` → `500` | 없는 칸으로 정렬. 지금은 막는 곳이 없음 (6장) |
| curl로 `keyword=펜`을 그대로 보냈더니 결과가 이상함 / `400` | 터미널에서 한글이 다른 인코딩으로 감. 브라우저를 쓰거나 `%ED%8E%9C`로 씀 (3-3) |
| `Port 8080 was already in use` | 이전 서버가 켜져 있음. 그 터미널에서 `Ctrl + C` |
| 4-7 화면에 `검색 실패: HTTP 502` | 백엔드(8080)가 꺼져 있음. 프록시가 못 닿으면 502 → 3-3대로 서버 켜기 |
| 4-7 Console에 `blocked by CORS policy` | 화면이 8080을 직접 부름 → 프록시로 (4-7 "이런 장면" 표). 백엔드 수정은 거절 |
| 4-7 `npm`을 못 찾음 | Node가 없거나 터미널을 Node 설치 전에 열었음. 새 터미널에서 `node -v` |
| 4-8 테스트 4개가 다 `ReferenceError: document is not defined` | 가짜 브라우저가 없음. 테스트 파일 **맨 위**에 `// @vitest-environment jsdom` (Claude Code 실측) |
| 4-8 하나만 `Found multiple elements with the text: 이전` | 앞 테스트의 화면이 남아 있음. `afterEach`에서 `cleanup()` (Claude Code 실측) |
| `git status`에 `node_modules/`가 보임 | 루트 `.gitignore`의 `node_modules/` 줄이 지워짐 (2-5) |

---

## 6. 알려진 한계 (면접에서 물어볼 만한 부분)
1. **조건이 늘면 메서드 이름이 폭발함**
   - "이름이 있으면 이름으로, 가격이 있으면 가격으로" 같은 **선택 조건**은 조합마다 메서드가 필요함(이름만 · 가격만 · 둘 다 …) → 10_04 Querydsl
2. **`like '%펜%'`는 인덱스를 못 탐**
   - 앞에 `%`가 붙으면 DB가 처음부터 끝까지 훑음. 데이터가 많으면 느림. `StartingWith`(`'펜%'`)나 전문 검색으로 바꿈
3. **`Page`의 count 쿼리**
   - 페이지마다 전체 개수를 다시 셈. 데이터가 많으면 count가 더 느릴 수 있음 → `Slice`, 또는 count 쿼리를 따로 가볍게 씀(`@Query(countQuery = ...)`)
4. **페이지 크기 상한이 없음**
   - `size=100000`을 보내면 그만큼 읽음. 스프링 기본 상한은 2000(`spring.data.web.pageable.max-page-size`). 서비스 규칙에 맞게 낮춰야 함
5. **잘못된 정렬 칸이 500**
   - `sort=prise`는 클라이언트 잘못이니 400이어야 함. 정렬 가능한 칸을 정해 두고 검사해야 함 → 10_06 표준 오류 응답
6. **벌크 수정은 엔티티 로직을 건너뜀**
   - `Product.changePrice()` 같은 엔티티 메서드 · 변경 감지를 거치지 않고 DB를 바로 바꿈. 엔티티에 규칙(최소 가격 등)이 있어도 우회됨. 영속성 컨텍스트가 낡는 문제는 `clearAutomatically`로 막지만, 비우기 전에 저장 안 된 변경이 있으면 사라질 수 있음(`flushAutomatically`)

---

## 7. 다 했으면
"가이드 03 다 했어"라고 알려 주세요. 이렇게 진행합니다.
1. 작성한 코드 리뷰 (4-1 · 4-2 · 4-3 · 4-4 "해 보기"에서 잠깐 넣은 코드가 남았는지도 확인)
2. `.\gradlew.bat check`와 서버 · 브라우저로 완료 기준 표대로 나오는지 함께 확인
3. 통과하면 기능 단위 커밋 (push는 직접)
4. 엑스트라(4-7 · 4-8)를 했으면 `day03/backend`가 안 바뀌었는지, `npm test` 4개 통과 · `npm run build` 통과를 함께 확인하고 `day03/frontend`를 따로 커밋

10_04 예고: 선택 조건 검색을 메서드 이름으로 만들면 조합이 폭발함 → Querydsl로 조건을 코드로 조립함

## 스스로 답해볼 질문
1. `existsByName`과 `countByName("…") > 0`은 결과가 같은데 SQL은 어떻게 다른가? 어느 쪽이 나은가
2. 메서드 이름 오타 하나에 왜 **저장소 테스트 전부**가 실패했나? 컨트롤러 테스트는 왜 통과했나
3. 결과가 여러 개일 수 있는 조회를 `Product`로 받으면 어떻게 되나? `Optional`은 언제 쓰나 (면접 질문)
4. `Page`와 `Slice`의 차이는? 무한 스크롤 화면에는 어느 쪽이 맞나 (면접 질문)
5. JPQL과 SQL의 차이는? `from Product`의 `Product`는 무엇의 이름인가
6. 프로젝션을 쓰면 무엇이 줄어드나? 보고서의 SQL로 설명해 보기
7. 벌크 수정 뒤 `findById`가 옛 값을 돌려준 이유는? `clearAutomatically`는 무엇을 하나 (면접 질문)
8. 응답으로 `Page`를 그대로 내보내면 왜 경고가 나오나? 화면 개발자 입장에서 무엇이 문제인가
9. (엑스트라) 에이전트에게 바로 "화면 만들어 줘"라고 하지 않고 ① API를 먼저 읽게 한 이유는?
10. (엑스트라) 프롬프트의 목표 · 위치 · 제약 · 완료 기준 중 왜 완료 기준이 가장 중요한가
11. (엑스트라) Vite 프록시를 쓰면 왜 CORS 문제가 안 생기나? 백엔드에 `@CrossOrigin`을 붙이는 것과 무엇이 다른가
12. (엑스트라) 테스트가 통과하는 것만으로 부족하고 일부러 망가뜨려 봐야 하는 이유는? (2일차 "단언 없는 테스트"와 연결해 보기)
