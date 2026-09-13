# Challenge Day 2/120 — 2026-09-10

발행: C002 / B002. 사용자 수행·시간·평가: 미제출. Benchmark/시장 조사일이 아니다.

## 먼저 고정하는 종료 기준

기본 90분(Coding 30 + Backend 45 + 회상 15) 또는 Minimum 45분(15 + 20 + 10)을 택한다.
구현 시작부터 시간을 재며 AI 환경 준비·설치·실행 대기는 제외한다.
시간 안에 시도한 코드/설명, 근거, 막힌 점을 제출하면 미완성이어도 일일 세션을 마감할 수 있다.
실력 Pass와 일일 종료는 별개다. 질문/오답으로 과제·시간을 늘리지 않는다. Day 1 미실시 구간은 이월하지 않는다.

## 실행 위치와 IntelliJ

주 작업본: `/Users/justin/IdeaProjects/my-project/study/challenge-120`.
이 대화의 체크아웃은 `/Users/justin/.codex/worktrees/ac86/my-project`로 별개다.
준비 때 두 위치에 반영했으나, 2026-09-10 정리 후 최신 코드·기록은 기존 IntelliJ 프로젝트에 유지한다.
worktree의 중복 변경분은 백업 후 제거했다. 이 대화의 Local 전환은 아직 대기 중이다.
이후 검증과 새 학습 대화도 기존 프로젝트를 사용한다.
기존 IntelliJ challenge-120 프로젝트에서 Gradle 새로고침만 한다. 처음 연결한다면 이 폴더의
`settings.gradle.kts`를 Gradle 프로젝트로 연결한다. Gradle JVM은 기존 JDK 21을 사용한다.
`.run`은 challenge-120을 IntelliJ 프로젝트 루트로 사용하는 기존 형식이다.
상위 my-project만 열었다면 아래 명령을 해당 디렉터리의 터미널에서 실행하거나 challenge-120을 연다.

| 대상 | 구현 파일 | IntelliJ 실행 구성 |
| --- | --- | --- |
| C002 | [LongestMonitoringSpan.java](src/main/java/challenge/LongestMonitoringSpan.java) | Day 002 - Coding |
| B002 | [CancellationService.java](src/main/java/challenge/CancellationService.java), 필요한 Repository | Day 002 - Backend |
| 전체 | 공개 테스트 전체 | Day 002 - All |
| Minimum | 같은 파일, 범위 축소 | Day 002 - Coding Minimum / Day 002 - Backend Minimum |

```sh
cd /Users/justin/IdeaProjects/my-project/study/challenge-120
./gradlew :day-002:test --tests challenge.LongestMonitoringSpanTest --rerun-tasks
./gradlew :day-002:test --tests challenge.CancellationServiceTest --rerun-tasks
# Minimum: 각 명령에 -Pminimum 추가
```

## 오늘의 과제와 진행

C002: 시간순 지연 관측값 중 합이 허용치 이하인 가장 긴 연속 구간의 길이. 세부 계약은 Java Javadoc에 있다.
먼저 `[3,1,2,4,1], allowance=6`을 손으로 풀고, 느려도 정확한 기본 풀이를 구현한다.
그 뒤 중복 계산을 찾고 최적화를 시도한다. 기본 정확성과 효율은 별도 평가한다.
자작 반례 한 개와 반복이 끝나는 이유를 남긴다. 큰 입력 테스트 시간 제한은 로컬 실용 검사이며 성능 벤치마크가 아니다.

B002: 존재하는 예약 취소 시 예약 삭제와 좌석 1개 반환. 재요청은 NOT_FOUND이며 추가 반환하지 않는다.
같은 예약의 동시 취소는 한 번 반환, 같은 이벤트의 다른 예약 두 개 취소는 좌석 두 개 반환.
저장 실패 시 두 변경은 모두 원복. 두 서비스 인스턴스가 같은 DB를 사용한다.
Spring/JPA 기본 틀은 제공됐으며 수동 트랜잭션 관리 코드는 요구하지 않는다.
트랜잭션/보호 범위와 두 요청의 교차 실행 순서를 직접 설명한다.

Minimum은 Coding의 작은 입력 기본 풀이, Backend의 순차·재요청·실패 원복 구현까지만 시도한다.
큰 입력 최적화 및 동시성 구현은 오늘 필수 종료 조건에서 제외한다. 동시 취소에서 위험한 교차 순서는 설명으로 남긴다.
Minimum 구성은 optimization/concurrency 태그만 제외한다. 미실시 범위를 Pass로 기록하지 않는다.

회상은 자료를 닫고 세 가지만 적는다(추가 문제 없음):
1. 내 Coding 풀이가 어떤 구간을 후보로 다루며, 반복은 왜 끝나는가?
2. 취소 두 요청이 겹칠 때 내 읽기/쓰기와 트랜잭션 경계가 최종 상태를 어떻게 결정하는가?
3. 저장 실패 뒤 DB에 무엇이 남아야 하며, 내 검증에서 아직 모르는 것은 무엇인가?

## Pass와 제출

- Coding 기본: 계약·작은 경계 입력 정확성, 자작 반례/종료/복잡도 설명. 최적화는 별도 판정.
- Backend: 선택한 수행 범위에서 반환 결과와 최종 DB 상태가 계약을 만족한다.
- 기본 90분에서는 동시 취소와 원복까지 확인하고 보호 대상·유지 기간을 코드와 연결해 설명한다.
- 제공 테스트와 새로 만든 검증, 독립 시도와 도움 후 수정은 구분한다. READ→WRITE 용어 재현만으로 가점하지 않는다.

제출: 선택한 90/45분 모드, 실제 시간, 초기 접근, 변경 코드와 테스트 결과,
자작 반례/교차 실행 순서, 위 짧은 회상, 막힌 점, AI·문서 도움 범위.
간단한 메시지로 제출해도 된다. 사용자가 요청하기 전 핵심 풀이를 제공하지 않는다.

## 준비 검증

검증 결과는 아래에 추가한다. H2 기능 검사만 제공하며 MySQL·성능·모든 동시 실행 순서는 미검증이다.
공개 테스트에 노출된 경계는 사용자가 독립 발견한 근거로 세지 않는다.

준비 검증 완료: 기존 Gradle Wrapper + JDK 21로 main/test 컴파일 및 실행.
전체 21개: 인프라 1개 통과, Coding 9개/Backend 기능 11개는 TODO 예외로 의도적 실패.
Minimum 14개: 인프라 1개 통과, 기능 13개는 TODO 예외로 의도적 실패; 큰 입력 1개/동시성 6개 제외 확인.
환경 오류 없음. `.run` 5개 XML과 task/경로를 확인했다. 사용자 수행 Pass를 뜻하지 않는다.
H2만 실행했으며 MySQL/실제 성능은 미검증.
