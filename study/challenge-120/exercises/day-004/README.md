# Challenge Day 4/120 · 2026-09-12

Day4 최초 발행. 달력상 Day4이며 완료한 학습일 수를 뜻하지 않는다. Day1~3에 제출/학습 근거가 있으나 전체 항목 완료·독립 Pass는 별도다. 현재 C004는 해답 설명 학습 기록이며 B004는 종료했다.

## 어제 결과와 오늘 목표

Day3 장부의 초기 상태보다 실제 코드가 앞서 있다. C003 괄호 검증과 optimistic-lab의 `@Version`, 일반 조회, 없는 이벤트 NOT_FOUND 구현을 확인했다. 기존 부하 비교는 인계된 학습 맥락이며 오늘 성능을 재측정하지 않는다. 반복 약점 해소·독립성·소요 시간은 새로 추정하지 않는다.

아래 Coding은 보존한다. 예전 Backend는 2026-09-13 사용자 요청으로 종료했다.

## C004 · 점검 시간 구간 합치기

구현: [MaintenanceWindows.java](src/main/java/challenge/MaintenanceWindows.java) · [공개 테스트](src/test/java/challenge/MaintenanceWindowsTest.java)

`int[][] merge(int[][] windows)`를 구현한다. 각 행은 `[start,end]`이고 입력 순서는 임의다. 겹치거나 맞닿은 구간은 하나로 합친다. 결과는 시작 시각 오름차순이며 더 합칠 구간이 없어야 한다.

- 입력: 0~100,000개, `0 <= start < end <= 1,000,000,000`. null/잘못된 행은 주어지지 않는다.
- 출력 예: `[[8,10],[1,3],[3,6],[2,4]] → [[1,6],[8,10]]`; 빈 입력은 빈 배열.
- 원본 배열·행을 수정하지 않는다. 반환 행과 입력 행을 공유하지 않는다.
- 포함된 구간, 같은 구간, 같은 시작점, 연쇄 접촉, 떨어진 구간을 구분한다.
- 학습 개념: 구간 관계, 결과 순서·불변조건, 입력 보존, 시간·공간 복잡도.
- 먼저 작은 입력을 손으로 풀고 정확한 기본 풀이를 작성한다. 이후 효율을 개선한다. 목표 O(n log n), 기본 정확성과 최적화는 별도로 평가한다.

## B004 — 종료한 실습

2026-09-13 사용자 요청으로 기존 예약·재시도 프로젝트와 부하 비교 실험을 정리했다.
B004는 현재 구현·실행 과제가 아니다. 원본 코드·테스트·문서와 별도 복구본은 사용자 요청으로 삭제했다.
종료 경위는 [9월13일 세션](../../../sessions/2026-09-13.md)에 기록했다.

## Coding 실행

```sh
cd /Users/justin/IdeaProjects/my-project/study/challenge-120
./gradlew :day-004:test
./gradlew :day-004:test -Pminimum
```

IntelliJ에서 `Day 004 - Coding`, `Day 004 - Coding Minimum`, `Day 004 - All`을 사용한다.
All은 현재 C004만 실행한다. 다음 Backend는 [Day 5 독립 결제 실습](../day-005/README.md)이다.
