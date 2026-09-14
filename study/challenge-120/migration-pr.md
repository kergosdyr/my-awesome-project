# feat: establish commerce app and reorganize challenge structure

날짜별 프로젝트와 예약·결제 lab으로 흩어진 챌린지를 공유 코딩 프로젝트와 하나의 FORM 커머스로 전환한다. 상품 선택부터 실제 주문·결제 요청·주문 내역까지 FE와 Spring API를 연결하고, Day6은 기존 Payment의 상태·승인 알림 구현을 이어간다.

## 변경 내용

- `exercises/day-*` 코딩 소스·테스트를 `src/{main,test}/java/challenge/coding`으로 통합했다. 중복 빌드 설정 원본은 `docs/archive/build-configs/*.txt`에 보존한다.
- 기존 예약·결제·HTTP 소스와 공개 테스트를 `legacy/{main,test}`로 이동했다. 추적 파일은 `git mv`를 사용하고 원본은 보존했다. legacy는 단일 Gradle 프로젝트의 별도 소스 세트이며 활성 앱의 classpath에는 포함하지 않는다.
- `challenge.commerce`에 API, 상품·주문·결제 업무, Reader/Saver, infra/db의 JPA 구현과 infra/pg의 가짜 PG를 구성했다. 실제 옵션 재고와 주문 가격 스냅샷을 제공하고 기존 결제 요청 정책을 주문에 연결했다.
- `src/main/resources/static`에 FORM 스토어와 PG 개발 도구를 추가했다. 같은 Spring 서버에서 FE·API를 실행하며 주문·결제·승인 대기·오류를 실제 응답으로 표시한다.
- Day6의 `Payment.confirmApproval`과 `PaymentService.onNotification`은 사용자 과제다. 기존 미완성 동작을 보존하고 상태·반복 알림 계약5개를 추가했다. 상품·주문은 별도 숙제가 아닌 제공 환경이다.
- README, 과제 설명, HTTP 요청·실행 클라이언트, IntelliJ 실행 구성을 현재 시스템에 맞췄다. Spotless는 활성 커머스·C006에 적용한다.

## 운영 방식

이 전환 PR 병합 이후 `day/NNN` 브랜치 → 학습·구현 → commit·PR → 리뷰·수정 → Squash and Merge로 진행한다. main에는 원칙적으로 하루당 하나의 squash commit을 남기며 과거 Git history는 다시 쓰지 않는다. 코딩 문제는 파일·테스트만 추가하고, 백엔드는 같은 커머스 시스템을 계속 발전시킨다.

## 검증

- `assemble`, 현재·과거 테스트 소스 컴파일, Spotless 통과.
- 커머스 제공 환경: H2/MySQL 각각9/9 통과. 서버 가격, 재고 차감·초과 주문 방지, 정상·반복 결제, 응답 유실 조회, PG와 로컬 상태 분리, HTTP·정적 파일을 확인했다.
- 기존 CI15개(C00510·결제 인프라5) 유지·통과. `ciTest`는 새 커머스 제공 환경9개도 실행한다.
- 기본 전체134개:111개 통과·23개 실패. 기존18개 실패는 동일하며 신규 Payment 상태1개·알림4개가 미완성으로 실패한다. 전체 `test`/`legacyTest`/`build`가 성공한다고 주장하지 않는다.
- 실제 Chrome 데스크톱·390px 모바일에서 상품 선택→주문→정상 결제, PG 승인→알림 오류→대기 유지, 가로 넘침·JavaScript 오류 없음을 확인했다.
- 활성 앱은 메모리 H2·가짜 PG·공용 주문 목록을 사용한다. 인증·실제 결제망·취소/환불·배송은 아직 구현하지 않았다.
