# 가이드 사용법

## 진행 방식
1. Claude Code가 일차마다 실습 폴더(`dayNN/`)와 빈 파일, 이 폴더의 가이드를 만듦. 가이드의 코드는 Claude Code가 먼저 직접 돌려 확인한 것임
2. 가이드를 보며 **빈 파일을 직접 채움** (복사·붙여넣기보다 직접 치는 것을 권장)
3. 다 쳤으면 "가이드 NN 다 했어"라고 알림 → Claude Code가 코드 리뷰와 실행 확인
4. 확인이 끝나면 기능 단위로 커밋함 (push는 직접)

- 실습 원본: `C:\Users\user\Desktop\실습\practice-20261112T040046Z-1-001\practice\10_0N` (N일차 = `10_0N`). 원본의 `backend-final` · 튜토리얼은 저장소에 넣지 않음
- 빈 파일에는 `package` 줄, import, 클래스 역할 주석, 비어 있는 선언만 있음. 클래스 안을 가이드 코드로 채움
- **일차는 순서대로 이어짐.** 다음 일차는 전날 완성 코드에서 시작함. 이 저장소는 2일차부터 시작해서 `day02`는 원본의 1일차 완성 코드(`10_02/backend-start`)를 가져와 둠

### 주석 규칙 (모든 가이드 공통)
1. **클래스 역할 주석**: 모든 클래스 맨 위에 아래 모양의 주석을 둠. 빈 파일에도 똑같이 넣어 둠
   ```java
   /**
    * 2일차 · 02 (커버리지 · TDD · AI 테스트 초안 검토)     ← 만든 일차 · 가이드 번호 (나중에 메서드를 더하면 같은 줄에 → 로 추가)
    *
    * [역할] 이 클래스가 하는 일 한두 줄
    *
    * [알아야 할 것]
    * - 이 클래스를 쓰기 전에 알아야 하는 개념, 다른 클래스와의 관계, 흔한 실수
    */
   ```
2. **중요한 줄 주석**: 가이드 코드의 중요한 줄에는 `//` 주석으로 뜻을 적음
   - 어노테이션(`@Test`, `@ParameterizedTest`, `@Component` …)이 무엇을 하는지
   - 단언 · 설정 한 줄 한 줄이 무엇을 확인하고 바꾸는지
   - 빠뜨리면 오류가 나는 줄은 그 오류 메시지
   - 단순한 import, getter, 괄호 같은 줄에는 달지 않음
- 설정 파일(`build.gradle`, `application.properties`)은 원칙적으로 Claude Code가 미리 고쳐 두고 가이드의 "미리 바꿔둔 설정"에 적음. **바꾸기 전후 차이를 보는 것이 실습이면** 4장에서 직접 바꿈 (예: 2일차 JaCoCo)

## 가이드 양식
모든 가이드는 아래 순서를 따름.

| 순서 | 제목 | 내용 |
|---|---|---|
| 맨 위 | 한 줄 요약 | 무엇을 무엇과 연결해서 무엇을 하는 기능인지 한두 줄 |
| | 목표 / 완료 기준 / 만들 API | 오늘 끝나면 되는 것, 확인할 결과 표, API 목록과 응답 예시 |
| 1 | 개념 먼저 | 처음 보는 사람 기준. 비유 → 뜻 → 오늘 코드에서 어디에 나오는지. 마지막에 용어 정리 표 |
| 2 | 미리 바꿔둔 설정 | Claude Code가 미리 고친 설정과 이유 (없으면 없다고 적음) |
| 3 | 실행 방법과 확인 도구 | 테스트 · 서버 켜기, 보고서 위치, curl. 4장 전에 한 번 읽음 |
| 4 | 단계별 진행 | 파일 트리 + 단계 표 → 단계마다 **할 일(코드) → 확인(실제로 나오는 결과)**. 위에서 아래로만 읽으면 됨 |
| 5 | 막히면 | 증상 → 원인 → 해결 표 |
| 6 | 알려진 한계 | 오늘 코드의 약점과 어느 일차에서 해결하는지 (면접 대비) |
| 7 | 다 했으면 | 끝나고 하는 일 + 스스로 답해볼 질문 |

- 4장의 확인에는 체크리스트만이 아니라 **실제로 나오는 값**(테스트 출력 · 응답 본문 · 로그 줄)을 같이 적음. Claude Code가 직접 돌려 받은 값임

## 실행 방법
이 PC는 PATH에 Java가 없어서 `JAVA_HOME`을 지정해야 함. PowerShell 기준:

```powershell
# dayNN\backend 폴더에서
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.12"
.\gradlew.bat test       # 테스트
.\gradlew.bat check      # 테스트 + 검사(커버리지 규칙 등)
.\gradlew.bat bootRun    # 서버 http://localhost:8080
```
- IntelliJ에서 실행하면 프로젝트 SDK로 Java 21을 지정해 두면 됨 (코드는 Java 17 문법 기준)

### IntelliJ에서 Gradle 프로젝트 연결하기
저장소 루트(`practice_SpringAdvanced`)를 열면 `build.gradle`이 `dayNN/backend` 아래에 있어서 IntelliJ가 Gradle 프로젝트로 인식하지 못함. 이 상태에서는 `import org.springframework...`에 빨간 줄이 생김 (코드 문제 아님. 터미널의 `gradlew`는 정상 동작)

일차마다 처음 한 번 연결함:
1. 프로젝트 창에서 `dayNN/backend/build.gradle` 오른쪽 클릭 → **Link Gradle Project**
   - 또는 오른쪽 Gradle 탭 → `+` → `dayNN/backend/build.gradle` 선택
2. 오른쪽 아래 동기화(Sync)가 끝날 때까지 기다림 → 빨간 줄이 사라짐
3. **File → Project Structure → Project SDK**가 Java 21(`ms-21.0.12`)인지 확인

- `build.gradle`을 고친 뒤에는 Gradle 탭의 새로고침(Reload) 버튼을 눌러야 바뀐 설정을 불러옴
- 연결이 번거로우면 File → Open으로 `dayNN/backend` 폴더만 따로 열어도 됨
- `src/main/java`에 **Mark Directory as → Sources Root를 누르지 않음**. 누르면 스프링 라이브러리가 없는 루트 모듈에도 소스가 등록돼 빌드 때 `package org.springframework... does not exist`가 남

## 가이드 목록
| 번호 | 일차 | 가이드 | 상태 |
|---|---|---|---|
| 02 | 2일차 (10_02) | [커버리지 · TDD · AI 테스트 초안 검토](02_커버리지_TDD_AI테스트검토.md) | 진행 전 |
