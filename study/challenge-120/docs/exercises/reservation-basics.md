# Day 1 — 직접 구현하는 Java 실습

Java 21. IntelliJ에서는 상위 `study/challenge-120` 폴더를 프로젝트로 엽니다. 최근 프로젝트의 `backend-challenge-120`을 다시 열면 됩니다. 날짜별 실습을 하나의 Gradle 프로젝트에서 관리합니다.

상단 실행 구성에서 `reservation-basics - Coding`, `reservation-basics - Backend`, `reservation-basics - All`을 선택하고 ▶를 누르세요. 테스트 파일의 클래스/메서드 옆 실행 버튼도 사용할 수 있습니다. Backend 기본 실행은 H2이므로 Docker가 필요 없습니다. Day 폴더가 추가되면 Gradle 새로고침으로 불러옵니다.

터미널에서는 아래 명령도 그대로 사용할 수 있습니다. 기존 앱 소스는 변경하지 않습니다.

## 먼저 Coding 25분

수정할 파일: [LongestBatch.java](../../src/main/java/challenge/coding/LongestBatch.java)

```bash
bash study/challenge-120/tools/reservation-basics.sh coding
```

`longestBatch(int[] costs, long budget)`는 합이 budget 이하인 연속 구간의 최대 길이를 반환합니다. 없으면 0입니다. 배열은 null이 아니고 길이 0~200,000, 원소 0~1,000,000,000, budget 0~10^15입니다. 입력을 변경하지 않습니다. 목표 시간 O(n), 추가 공간 O(1). 알고리즘은 직접 선택합니다.

예: [4,2,1,7,3], 8 → 3 / [0,0,5], 0 → 2 / [], 10 → 0.

공개 테스트 8개를 제공합니다. 이를 통과하는 것 외에 본인이 생각한 경계 테스트를 추가하고, 복잡도를 설명하세요. 시간 복잡도는 실행 시간만으로 증명하지 않습니다.

## 다음 Backend Practical 25분

수정할 파일: [ReservationService.java](../../legacy/main/java/challenge/reservation/basic/ReservationService.java), 필요하면 [schema.sql](../../legacy/main/resources/reservation/reservation-basics/schema.sql).

```bash
bash study/challenge-120/tools/reservation-basics.sh backend
```

Spring Boot + Spring Data JPA 실습입니다. H2가 자동 실행되며 별도 DB 설치가 필요 없습니다. `@SpringBootTest`와 두 번째 Boot 컨텍스트가 같은 DB를 사용합니다.

제공: `Event`/`Reservation` 엔티티, `JpaRepository` 인터페이스, `@Service` 생성자 주입, `reserve()`의 기본 `@Transactional`. 수동 트랜잭션 매니저/Template 코드를 작성할 필요가 없습니다. Repository 메서드, 예약 로직, 동시성 전략과 재요청 처리를 직접 구현하세요. 트랜잭션 설정은 필요에 따라 수정할 수 있습니다.

스키마는 `schema.sql`로 초기화하고 Hibernate 자동 DDL은 끕니다. 제약·인덱스 변경은 해당 파일에도 반영하세요. 테스트의 JdbcTemplate은 초기 데이터·장애 주입·DB 결과 확인에만 사용합니다. 테스트 메서드 자체에는 @Transactional을 붙이지 않아 서비스 호출의 실제 커밋/롤백 결과를 확인합니다.

요구사항: 같은 이벤트에 사용자당 예약 최대 1건. 성공하면 예약 생성과 remaining 감소가 함께 반영. 실패하면 변경 없음. 초과 예약 금지. 재요청은 기존 예약을 반환하고 추가 차감하지 않음. 취소·결제는 범위 밖입니다.

반환 계약:
- CREATED: 새 예약 ID 반환.
- EXISTING: 기존 예약 ID 반환. 매진 후 재요청도 동일합니다.
- SOLD_OUT: 예약 불가, reservationId는 null로 반환하세요.
- DB 저장 장애: Spring DataAccessException을 호출자에게 전달하고 데이터 변경은 남기지 않습니다.
- 테스트에서는 이벤트가 존재하고 userId는 양수입니다. 미존재 이벤트 정책은 오늘 평가 범위 밖입니다.

공개 테스트: JPA 매핑·두 컨텍스트 연결 확인 1개(미구현 상태에서도 통과), 정상 예약, 매진, 순차 재요청, DB 저장 실패, 다른 사용자 12명 동시 요청, 같은 사용자 12개 동시 재요청. 동시 테스트는 각각 3회 수행합니다. CHECK 제약으로 저장 실패를 주입하며 테스트 전용 userId를 구현에서 특별 취급하지 마세요.

H2의 MySQL 모드는 실제 MySQL의 락/격리 동작과 같지 않습니다. 두 컨텍스트도 서로 다른 OS 프로세스는 아닙니다. 같은 JVM에서 통과했다는 사실만으로 서버 간 안전성을 입증하지 않으며 static/global JVM 락에만 의존하는 구현은 요구사항을 만족하지 않습니다. 반복 테스트 통과도 모든 실행 순서를 증명하지 않습니다.

## MySQL 8로 확인

Docker/OrbStack 실행 후 이 실습 폴더에서 아래 명령을 사용하세요. 13316 포트의 전용 실습 DB이며 테스트마다 event/reservation 테이블을 초기화합니다. 다른 DB 주소로 바꾸지 마세요. 비밀번호는 이 로컬 일회용 실습 전용 값입니다.

```bash
docker compose up -d --wait
CHALLENGE_JDBC_URL='jdbc:mysql://127.0.0.1:13316/challenge_day001?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000' CHALLENGE_DB_USER=challenge CHALLENGE_DB_PASSWORD=challenge bash tools/reservation-basics.sh backend --rerun-tasks
docker compose down -v
```

DB를 바꿀 때는 Gradle 캐시 때문에 테스트가 생략되지 않도록 `--rerun-tasks`를 붙입니다. 현재 준비 시점에는 Docker 데몬이 꺼져 있어 MySQL 실행은 미검증입니다.

## 실행 결과와 제출

미구현 상태의 TODO 예외와 테스트 실패는 의도된 시작 상태입니다. 컴파일 실패·DB 접속 오류와 구분하세요. 전체 테스트는 `bash tools/reservation-basics.sh all`, 컴파일만 확인하려면 `bash tools/reservation-basics.sh compile`입니다(챌린지 루트 기준).

[설계·장애 분석·프로젝트 설명을 포함한 전체 문제](../../../sessions/2026-09-09.md)는 그대로 유지합니다. 이 폴더의 코드나 schema 수정 후 파일 경로와 실제 시간을 보내면 평가합니다. 테스트를 통과시키기 위해 제공 테스트의 기대값을 바꾸거나 제거하지 마세요. 계약 오류가 의심되면 질문하세요.

제공된 scaffolding과 공개 테스트는 풀이 도움이 아니며 독립성 감점에서 제외합니다. 제공 케이스를 읽은 것은 독립적으로 경계 조건을 발견한 증거로 계산하지 않습니다. 본인이 추가한 테스트, 초기 구현, 참고한 자료를 따로 남겨주세요. 준비 과정의 빌드 시간은 사용자 문제 풀이 시간에 포함하지 않습니다.
