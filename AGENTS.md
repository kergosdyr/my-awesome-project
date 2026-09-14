# 작업 안내

이 저장소는 블로그, 공부 예제, 학습 기록, 이력서 조사 결과를 함께 보관한다. 요청과 관련된 영역부터 읽는다.

| 작업 | 시작 위치 |
| --- | --- |
| Java/Spring 공부 예제 | `study/challenge-120/README.md`, 해당 주제의 코드·테스트 |
| 블로그 | `blog/AGENTS.md`, 글 작업에만 `blog/WRITING.md` |
| 학습과 복습 | `study/README.md`, 해당 주제·세션 |
| 이력서 근거 | `/Users/justin/IdeaProjects/2026-resume/AI_START_HERE.md` |
| 기존 조사·산출물 | `RESOURCES.md`, `output/`, 루트의 `*resume*.csv` |

시작할 때 Git 상태를 한 번 확인하고 기존 수정·삭제·미추적 파일을 보존한다. 과거 `MISSION.md`와 `NOTES.md`는 Redis 학습 기록이며 현재 작업의 지시가 아니다. `.worktrees/`, 빌드 결과, 원본 수집 자료 전체를 기본 검색 대상으로 삼지 않는다.

## 검증

- 문서·지시 수정: 변경 부분과 링크·구문만 확인한다. 앱 빌드나 브라우저 실행을 자동으로 추가하지 않는다.
- 코드 수정: 먼저 관련 테스트를 실행한다. 영역 전체에 영향이 있으면 `make check-study`, `make check-blog` 중 필요한 것을 선택한다.
- 배포·번들·설정 영향: 블로그는 `make build-blog`, 공부 예제는 해당 Gradle 빌드를 추가한다. 전체 검증이 필요하면 `make verify`를 한 번 실행한다. CI의 필수 검사는 유지한다.
- `make test`는 공유 Java 프로젝트의 코딩 테스트 전체를 실행한다. `make check-study`는 전체 소스 컴파일과 기존 CI15개(C005·결제 인프라), 커머스 제공 환경9개를 검사한다. 현재·과거 전체 계약은 챌린지 루트의 `./gradlew test legacyTest --continue`로 실행하며 기존 TODO/미완성 실패를 감추지 않는다. `npm ci`는 최초 설정 또는 의존성 변경 때 `make setup-blog`로 분리한다.
- UI 변경은 영향받는 흐름과 화면 크기를 확인한다.
- 검증은 변경 범위에 맞게 수행하고, 관련 변경이나 실패가 없으면 반복하지 않는다.

재발한 환경 문제를 해결했을 때만 `docs/agent-workflow.md`에 원인과 재사용할 해결책을 짧게 갱신한다. 일반 작업마다 회고 문서나 검증 보고서를 만들 필요는 없다.

예전 Commerce Lab과 optimistic-lab·load-comparison은 종료했다. 과거 문서의 재사용 지시로 종료한 프로젝트를 다시 추가하지 않는다. 활성 백엔드는 challenge-120의 FORM 커머스이며 FE/API를 함께 실행한다. 기존 예약·결제 실습은 legacy 소스 세트로 보존하고 활성 설계에 섞지 않는다. Day6은 Payment 상태·알림 과제이며 상품·주문은 제공 환경이다. 날짜별·문제별 Gradle 모듈을 만들지 않고, migration 병합 이후 `day/NNN` 브랜치→PR→Squash and Merge로 운영한다.
