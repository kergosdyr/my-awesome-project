# 120일 챌린지 — Coding / FORM Commerce

[과제 목록](docs/exercises/README.md) · [C007 사이즈 탐색](docs/exercises/C007-SizeSearch.md) · [B007 가격 확인](docs/exercises/B007-price-consent.md) · [학습 장부](ledger.md)

## 지금 시작할 Day 9

- **Backend45분:** [B009 — 주문 목록 이어 읽기와 조회 비용](docs/exercises/B009-OrderPaging.md). 새 주문 삽입·깊은 페이지·인덱스 실행계획을 현재 FORM 데이터로 관찰한다. `afterCreatedAt`·`afterId`와 타입 변환은 제공한다. `OrderQueryService.window`를 연결하고 Q클래스 조회 조건·인덱스를 직접 구현한다.
- **Coding30분:** [C009 — 가장 큰 관측값 k개](docs/exercises/C009-LargestReadings.md). `LargestReadings.topK`를 구현한다.
- 통합 복습15분은 이미 발행·응답한 기존 기록을 유지한다. 새 문항을 중복 발행하지 않는다. Minimum45분은 각 과제15/20분과 기존 복습10분이다.
- C008은 해답 설명 후 사용자 구현8/8·마무리 선언, B008은 AI 해답 구현 후12/12·대략 이해 자기보고다. 독립 해결로 평가하지 않는다. Day9는 사용자 요청으로 전환했으며 과거 미완료 항목을 추가 숙제로 붙이지 않는다.

```sh
./gradlew :commerce:pagingExperiment
./gradlew :commerce:test --tests '*OrderWindowTest'
./gradlew :coding:test --tests '*LargestReadingsTest'
```

B009 조회 결과 검사는 사용자 구현과 조건 수정 피드백 이후7/7 통과했다. 인덱스 실험 파일은 아직 비어 있다. C009는 AI 해설·예제 코드를 참고해 우선순위 큐 풀이를 작성했다. 현재 결과 배열 반복문의 증가 방향 오류로8개 검사가 실패하며, 독립 해결이나 완료로 평가하지 않는다. 제공 환경 검사와 과제 완료를 구분한다.

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
./gradlew ciTest                                     # 완료 코딩28 + 커머스44
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

트랜잭션은 Service가 소유하고 관리 중인 Entity 변경을 반영한다. OSIV=false이며 상품/옵션 목록은 SQL2회, 주문 목록은 페이지 ID 조회 후 주문·품목·결제 조인으로 SQL2회에 읽는다. 상품 목록도 상품 ID 페이지 조회 후 상품·옵션 조인으로 읽는다. 주문 조회는 기본20개·최대100개로 제한한다. JSON은 Entity를 직접 노출하지 않고 DTO로 변환한다. 개발용 PG 제어 API의 직접 infra 참조는 기존 실습 도구 범위다.

## 현재 검증과 남은 과제

2026-09-16: 모듈 컴파일·패키징과 필수 CI는 통과. Coding 전체66개 중62개 통과·기존 C004 네 개 실패. B007은 사용자 가격 비교 구현에 요청받은409 예외 연결을 AI가 적용한 뒤8/8 통과했다. 사용자가 작성한 가격 조건과 빈 ProductValidator는 보존했다. 전체 build 성공으로 표시하지 않는다.

예전 CI의 C00510개는 유지하고 완료된 C006·C007각9개를 추가했다. 삭제 요청된 legacy 결제 검사5개는 함께 제거했으며 활성 커머스44개(제공 환경9·결제5·응답3·조회2·다품목10·기본 페이지6·Entity 조인6·이어 조회 입력/응답3)를 필수 검사로 실행한다. 미완성 과제는 전체 test에서 계속 드러난다.

## 운영

`day/NNN → PR → Squash and Merge`. 날짜는 Git·학습 기록으로 관리한다. 기본90분/Minimum45분과 Phase·주간 리뷰는 [학습 가이드](docs/learning-guide.md)를 따른다.

사용자 요청으로 `legacy/`, 종료한 B001~B005 실행 문서, 이전 구조 이전 PR 초안, 오래된 scaffold·부하 도구를 삭제했다. 현재 과제와 학습 세션·장부는 보존하며 과거 자료 링크는 필요할 때 당시 Git 커밋으로 연결한다. 제거한 레거시를 후속 과제에서 자동 복원하지 않는다.


## 다품목 주문 · 2026-09-16

`OrderEntity`는 주문 전체, `OrderItemEntity`는 품목별 구매 당시 정보를 보관한다. API 입력은 `items: [{optionId, quantity, displayedUnitPrice}]`, 응답은 `id/items/totalAmount/createdAt`이다. 품목1~20개·옵션별 수량1~5개이며 중복 옵션 행은400이다. 화면에서는 장바구니에 여러 상품을 담아 함께 주문하고 총액으로 결제한다. 품목별 가격 불일치나 재고 부족이면 주문 전체를 취소한다. 상품·옵션은 선택한 ID로 일괄 조회하고, 주문 상세·목록은 품목을 함께 읽는다.

검증: 커머스 전체37개, 필수 CI56개(coding28+commerce28), 패키징·포맷 통과. 다품목 가격 상쇄·재고 롤백·금액 보존·중복 입력·응답 조회를 포함한다. 실제 브라우저1280px/390px에서 장바구니·주문·결제와 수량 수정·삭제·가격 재확인을 확인했다. 기존 Coding C004 실패4개는 이번 변경과 무관하며 전체 코딩 테스트를 다시 실행하지 않았다.

재검토 한계: 가격 조회 이후의 동시 가격 변경·견적 유효기간은 별도 정책이 필요하다. 목록 페이지 분할·장바구니 영구 저장은 아직 없다. 현재 H2 제공 환경은 재기동 때 생성되므로 운영 데이터 이전 스크립트는 포함하지 않는다.

## 기본 주문 페이지네이션 · 2026-09-18

`GET /api/orders?page=0&size=20`는 `{entries,page,size,hasNext}`를 반환한다. 기본20개·최대100개, page는0부터이며 createdAt/id 내림차순이다. 이전의 JSON 배열 응답을 페이지 DTO로 변경했으며 화면과 기존 검사를 함께 수정했다. 주문 ID에만 Pageable의 offset·size와 QueryDSL의 조회 제한을 적용하고 해당 품목·결제만 일괄 조회한다. FORM의 이전·다음 이동과 첫 페이지 새로고침까지 제공 기능이다. `readAll()` 기반 주문 목록은 제거했다.

페이지네이션 자체는 과제가 아니다. B009는 완성된 OFFSET 조회에서 새 주문 삽입·깊은 위치의 DB 비용을 개선하는 과제로 유지한다. 기존 ‘목록 페이지 분할 없음’ 기록은9/16 당시의 한계이며 이 변경으로 해소했다. 실제 DB 정렬·스캔 최적화까지 완료한 것은 아니다.

## Entity 조합 조회 · 2026-09-18

주문·결제와 상품·옵션의 조인은 infra에서 QueryDSL Q클래스와 명시적인 ON 조건으로 처리한다. `JPAQueryFactory`는 infra에서 주입하고 문자열 HQL·문자열 별칭 대신 Q필드와 `Tuple.get(Q타입)`을 사용한다. QueryDSL 7.1(OpenFeign 배포판)의 annotation processor가 컴파일 시 `commerce/build/generated/sources/annotationProcessor/java/main`에 Q클래스를 생성한다. 생성 파일을 직접 작성하거나 Git에 넣지 않는다. [QueryDSL JPA 설정 문서](https://openfeign.github.io/querydsl/tutorials/jpa/)를 따른다. `OrderDetailsResult`는 OrderEntity/PaymentEntity, `ProductResult`와 `ProductSelectionResult`는 기존 상품/옵션 Entity를 그대로 담는다. 서비스·Reader의 별도 일괄 조회와 맵 조합을 제거했고 중간 OrderSlice도 제거했다. 결과를 계층마다 복제하지 않으며 API에서 HTTP DTO로 변환한다. 결제 없는 주문의 UNPAID 표현도 API에서 결정한다.

상품 목록은 `/api/products?page=0&size=20`으로 조회하며 기존 배열 응답을 유지한다. 기본20·최대100개, 상품 ID 오름차순이다. 화면은 더 보기로 추가 페이지를 읽고, 장바구니 가격 재확인도 이미 읽은 페이지 범위를 나눠 조회한다. 페이지가 정확히20개로 끝나면 한 번 더 빈 페이지를 확인한 뒤 더 보기 버튼이 사라진다. 주문·상품의 범위 검증은 공통 PageQuery를 사용한다.

생명주기를 함께 관리하는 Order–OrderItem의 cascade·연관관계는 유지한다. 조회를 합치기 위해 Product–Option이나 Order–Payment에 cascade를 추가하지 않는다. 주문 품목 스냅샷 생성·가격 판단·재고 차감·쿠폰 계산·외부 PG 처리는 기존 업무 객체에 남긴다. 조회한 Entity는 호출 측 쓰기 트랜잭션에서 변경할 수 있으며, OSIV=false와 조회 서비스의 readOnly 경계는 유지한다.
