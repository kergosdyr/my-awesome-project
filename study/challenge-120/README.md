# 120일 챌린지 — Coding / FORM Commerce

[과제 목록](docs/exercises/README.md) · [C007 사이즈 탐색](docs/exercises/C007-SizeSearch.md) · [B007 가격 확인](docs/exercises/B007-price-consent.md) · [학습 장부](ledger.md)

## 작업 구조

하나의 IntelliJ 프로젝트·Gradle Wrapper 아래 **서로 의존하지 않는 두 모듈**을 둔다.

```text
challenge-120/
├── coding/                 # Java 코딩 풀이·JUnit 테스트. Spring/DB 의존성 없음
│   ├── build.gradle.kts
│   └── src/{main,test}/java/challenge/coding/
├── commerce/               # FORM의 Spring Boot·JPA·API·화면
│   ├── build.gradle.kts
│   └── src/{main,test}/...
├── docs/exercises/         # C001-…md, B006-…md 등 과제별 문서
├── docs/learning-guide.md  # 현재 운영 원칙
├── requests/payment.http
├── tools/payment-flow.py
└── .run/                   # 현재 모듈·과제에 맞는 IntelliJ 실행 구성
```

기존 IntelliJ 프로젝트 `/Users/justin/IdeaProjects/my-project/study/challenge-120`를 계속 사용한다. 날짜별 모듈·새 worktree는 만들지 않는다. 새 대화도 이 원래 checkout을 공유한다.

## 실행과 검사

모든 명령은 이 폴더에서 실행한다. Java21과 기존 Wrapper를 사용한다.

```sh
./gradlew :coding:test --tests '*SizeSearchTest'      # C007
./gradlew :coding:test                               # 코딩 전체
./gradlew :commerce:run                              # FE/API 함께 실행
./gradlew :commerce:test --tests '*PriceConsentTest'  # B007
./gradlew :commerce:priceExperiment                  # 가격 변경 관찰
./gradlew :commerce:test --tests 'challenge.commerce.Payment*' # B006
./gradlew ciTest                                     # 완료 코딩28 + 커머스18
./gradlew test --continue                            # 모든 현재 과제; 미완성 실패도 표시
./gradlew assemble                                  # 두 모듈 컴파일·커머스 실행 배포본
./gradlew build --continue                           # 전체 테스트·패키징·포맷
./gradlew spotlessCheck
./gradlew :commerce:ciTest -Pmysql                    # Docker 필요, 선택 검사
```

IntelliJ 구성: `C007 - SizeSearch`, `B007 - Price Consent`, `B007 - Observe`, `Commerce - Server`, `Commerce - CI`, `Coding - All`.

- 스토어: <http://127.0.0.1:18086/>
- 개발용 PG: <http://127.0.0.1:18086/dev.html>
- H2·가짜 PG는 메모리 기반이며 서버 재시작 시 초기화된다. 회원·실제 결제·배송은 없다.

## API와 업무 객체 경계

HTTP Request/Response는 `commerce/.../api`에 둔다. Command·Query·Result는 업무 입력·조합 결과로 필요한 경우에만 만든다. JPA Entity는 Service·Reader·Saver·Validator와 API 응답 매퍼가 공유할 수 있는 유일한 infra 예외다. Entity 복제 모델과 왕복 변환은 만들지 않는다. JpaRepository·EntityManager·쿼리·DB 설정은 infra에 유지한다.

트랜잭션은 Service가 소유하고 관리 중인 Entity 변경을 반영한다. OSIV=false이며 상품/옵션·주문/결제의 목록 조회는 각각 SQL2회다. JSON은 Entity를 직접 노출하지 않고 DTO로 변환한다. 개발용 PG 제어 API의 직접 infra 참조는 기존 실습 도구 범위다.

## 현재 검증과 남은 과제

2026-09-16: 모듈 컴파일·패키징과 필수 CI는 통과. Coding 전체66개 중62개 통과·기존 C004 네 개 실패. B007은 사용자 가격 비교 구현에 요청받은409 예외 연결을 AI가 적용한 뒤8/8 통과했다. 사용자가 작성한 가격 조건과 빈 ProductValidator는 보존했다. 전체 build 성공으로 표시하지 않는다.

예전 CI의 C00510개는 유지하고 완료된 C006·C007각9개를 추가했다. 삭제 요청된 legacy 결제 검사5개는 함께 제거했으며 활성 커머스18개(제공 환경9·결제5·응답2·조회2)를 필수 검사로 실행한다. 미완성 과제는 전체 test에서 계속 드러난다.

## 운영

`day/NNN → PR → Squash and Merge`. 날짜는 Git·학습 기록으로 관리한다. 기본90분/Minimum45분과 Phase·주간 리뷰는 [학습 가이드](docs/learning-guide.md)를 따른다.

사용자 요청으로 `legacy/`, 종료한 B001~B005 실행 문서, 이전 구조 이전 PR 초안, 오래된 scaffold·부하 도구를 삭제했다. 현재 과제와 학습 세션·장부는 보존하며 과거 자료 링크는 필요할 때 당시 Git 커밋으로 연결한다. 제거한 레거시를 후속 과제에서 자동 복원하지 않는다.
