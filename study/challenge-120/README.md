# 120 Days Challenge

120일 동안 꾸준히 개발 역량을 쌓기 위한 챌린지.

코딩 테스트는 공유 Java 프로젝트에서 독립적으로 풀고, 백엔드는 **FORM이라는 하나의 패션 커머스**를 계속 발전시킨다. 상품·옵션 조회 → 주문 생성 → 결제 요청 → PG 승인 알림 → 주문 내역을 실제 화면과 API로 연결했다. 무신사 같은 구매 경험을 참고한 작은 학습 시스템이다.

## 실행

Java21과 기존 Gradle Wrapper를 사용한다. 별도 FE 설치나 서버 없이 Spring이 HTML/CSS/JavaScript와 API를 함께 제공한다.

```sh
cd study/challenge-120
./gradlew run
```

- 스토어: <http://127.0.0.1:18086/> — 상품·옵션 선택, 주문, 결제, 주문 내역
- 개발 도구: <http://127.0.0.1:18086/dev.html> — PG 정상/대기/응답 유실, 승인 완료, HTTP 알림 재전송
- IntelliJ: 이 폴더를 Gradle 프로젝트로 열고 `Commerce - Server` 실행

서버는 loopback에 바인딩한다. H2와 가짜 PG는 메모리 기반이므로 재시작하면 초기화된다. 현재는 단일 실행 환경의 공용 주문 목록이며 회원·인증·실제 PG·배송·취소를 구현하지 않았다.

## 현재 구현과 Day 7

상품 3개와 색상·사이즈 옵션, 옵션 재고, 주문 가격 스냅샷을 제공한다. 한 옵션을 1~5개 주문하며 서버 가격으로 금액을 계산한다. 조건부 재고 차감과 주문 생성을 한 트랜잭션에 묶었다.

결제 요청에는 주문 ID만 사용한다. 기존 사용자의 정상 승인·응답 유실 직후 PG 조회 정책을 실제 주문과 연결했다. 주문 내역의 결제 상태는 저장된 Payment에서 읽고 결제 행이 없으면 UNPAID로 표시한다. FE는 정상 결제·대기·오류를 서버 결과 그대로 보여준다.

**Day 7은 할인 종료 직후의 가격 확인 실험이다.** [과제·예상 질문·실행](docs/exercises/price-consent.md)에서 시작한다. 화면 가격 전달과 HTTP 재현 환경은 제공했으며, 가격 변경 판단은 사용자 구현 대상으로 남아 있다. 원래 IntelliJ 프로젝트의 `day/007`에서 진행한다.

Day6 사용자 `Payment.confirmApproval`·`PaymentService.onNotification` 구현은 보존했고 상태·알림5/5, C006 9/9 통과를 확인했다. 자료 없는 설명·독립 회상 성공은 별도 미검증이다.

## 구조

```text
study/challenge-120/
├── build.gradle.kts / settings.gradle.kts / gradlew / gradle/
├── src/
│   ├── main/java/challenge/
│   │   ├── coding/                    # 공유 코딩 문제
│   │   └── commerce/
│   │       ├── CommerceApplication.java
│   │       ├── api/                   # 상품·주문·결제 Controller
│   │       ├── domain/
│   │       │   ├── catalog/           # 상품·옵션·재고
│   │       │   ├── order/             # 주문 생성·조회
│   │       │   └── payment/           # 결제·Reader/Saver·PG 계약
│   │       ├── infra/
│   │       │   ├── db/                # JPA Entity·Repository·구현·fixture
│   │       │   └── pg/                # 가짜 PG 원장
│   │       └── support/               # 업무 오류
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── static/                    # FORM FE·개발 도구·로컬 SVG
│   └── test/java/challenge/
│       ├── coding/
│       └── commerce/                  # 제공 HTTP 환경·Day6 계약
├── legacy/
│   ├── main/                         # 기존 예약·결제·HTTP 원본
│   └── test/                         # 기존 계약·부하 실행 소스
├── requests/payment.http
├── tools/                            # 현재/과거 HTTP·DB·부하 실행 도구
├── .run/                             # Commerce·Coding·Legacy 실행 구성
└── docs/                             # 주제별 과제·초기 시도·이전 설정
```

`Controller → Service → Reader/Saver·업무 객체 → 저장소 계약`으로 연결하고 JPA와 PG 구현은 `infra`가 맡는다. 업무 상태는 Payment가 소유하고 트랜잭션은 Service가 소유한다. Reader/Saver는 조합 가능한 구체 클래스이며 불필요한 interface/Impl 쌍은 만들지 않는다. 주문 목록의 결제 조회는 일괄 조회한다.

활성 앱에는 `reservation/lab`이 없다. [legacy](legacy/README.md)는 과거 학습 원본을 삭제하지 않고 실행 가능하게 보존한 소스 세트다. **하나의 Gradle 프로젝트** 안에 있으며 별도 모듈이나 앱 의존성이 아니다. 현재 상품·주문은 예약 클래스의 이름만 바꾼 모델이 아니다. 종료한 예전 Commerce Lab은 복원하지 않았다.

## Workflow

1. `day/NNN` 브랜치를 생성한다.
2. 그날의 학습 또는 구현을 진행한다.
3. commit하고 Pull Request를 생성한다.
4. 필요한 리뷰 및 수정을 진행한다.
5. Squash and Merge한다.
6. main에는 원칙적으로 하루당 하나의 squash commit을 남긴다.

이번 전환 PR이 병합된 이후부터 적용한다. 과거 Git history는 다시 쓰지 않는다. 다음 날에는 최신 main에서 `day/007`처럼 시작하고 같은 시스템을 수정한다. PR에 학습 주제·변경·검증·남은 질문을 남긴다.

## Principles

- 날짜는 디렉터리가 아니라 Git history로 관리한다.
- 디렉터리는 코드의 역할과 주제를 기준으로 구성한다.
- 간단한 코딩 테스트 문제는 기존 프로젝트에 파일과 테스트만 추가한다.
- Pull Request를 하루의 학습 기록으로 사용한다.
- 날짜별·문제별 Gradle 프로젝트와 모듈을 추가하지 않는다.
- 학습 원본과 초기 시도는 보존하고, 사용자 과제의 답안을 미리 채우지 않는다.

## Build and Test

```sh
./gradlew assemble testClasses legacyTestsClasses # 현재·과거 컴파일과 실행 배포본
./gradlew commerceInfrastructureTest              # 커머스 제공 환경 9개
./gradlew ciTest                                  # 위 9개 + 기존 필수 15개
./gradlew test --tests 'challenge.commerce.Payment*' # Day6 상태·알림 5개
./gradlew priceExperiment                         # Day7 현재 가격 변경 동작 관찰
./gradlew test --tests '*PriceConsentTest'         # Day7 Backend 공개 8개
./gradlew test --tests '*SizeSearchTest'           # Day7 Coding 공개 9개
./gradlew test --tests 'challenge.coding.*'
./gradlew test legacyTest --continue              # 현재·과거 기본 공개 계약 전체
./gradlew build --continue                        # 위 계약 + 패키징·포맷
./gradlew test -Pminimum                          # 기존 축소 태그 유지
./gradlew commerceInfrastructureTest -Pmysql      # Docker 필요, 테스트용 MySQL
./gradlew test -Pmysql --tests 'challenge.commerce.Payment*'
./gradlew legacyTest --tests 'challenge.payment.*'
./gradlew legacyTest --tests 'challenge.lab.*'
./gradlew spotlessApply spotlessCheck
python3 tools/payment-flow.py                    # 실행 중인 서버에 요청·알림
```

Day7 준비 검증: 컴파일·assemble 성공, H2 커머스 제공 환경9/9·기존 필수15/15·Day6 상태/알림5/5 통과. 가격 관찰 실험1/1 실행 성공. 새 과제는 가격 확인4/8 통과·4개 의도된 실패, C007은9/16 해답 설명 후 사용자 구현9/9 통과로 갱신했다. 전체·MySQL 검사를 이번에 다시 실행하지 않았고 전체 build 성공으로 표시하지 않는다. `make check-study`의 필수 CI 검사는 유지한다.

## 학습 기록

- [Day 7 할인 종료 가격 실험·사이즈 탐색](docs/exercises/price-consent.md)
- [Day 6 코딩·커머스 결제](docs/exercises/payment-http.md)
- [Phase·시간·평가/튜터 원칙](docs/learning-guide.md) · [기존 장부](ledger.md)
- [이전 예약·결제 실습 실행](legacy/README.md)
- [전환 PR 제목·본문](migration-pr.md)

기본90분/Minimum45분, Weekly Review·Benchmark·사용자 선행 시도 원칙은 유지한다. 백엔드의 맥락은 커머스로 이어가되 매일의 필수 범위는 해당 과제에서 제한한다.
