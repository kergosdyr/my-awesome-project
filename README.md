# 블로그와 공부 예제

기술 블로그와 직접 풀어보는 Java/Spring 공부 예제, 날짜별 학습 기록을 보관합니다.

- [120일 챌린지](study/challenge-120/README.md) · [FORM 커머스·Day6 결제](study/challenge-120/docs/exercises/payment-http.md)
- [학습 기록과 복습 운영](study/README.md)
- [블로그](blog/README.md)
- [기존 조사·산출물](RESOURCES.md)

## 시작하기

Java 21을 사용합니다. IntelliJ에서 `study/challenge-120`을 Gradle 프로젝트로 열면 하나의 Java 프로젝트에서 주제별 코드와 테스트를 실행할 수 있습니다. 날짜는 day/NNN 브랜치와 PR로 관리합니다.

```sh
make test                 # 공유 프로젝트의 코딩 테스트 전체
make check-study          # 전체 컴파일 + 기존 CI15개·커머스 환경9개
make setup-blog           # 최초 설정 또는 의존성 변경 때만
make check-blog           # 블로그 lint·typecheck
make build-blog           # 블로그 배포 빌드 (배포하지 않음)
```

결제 과제의 TODO를 포함한 전체 계약은 별도로 실행합니다. 미구현 TODO 실패는 제공 환경 실패와 구분합니다.

```sh
cd study/challenge-120
./gradlew run        # FORM 스토어: http://127.0.0.1:18086/
./gradlew test legacyTest --continue
./gradlew commerceInfrastructureTest -Pmysql
```

## 저장소 구조

| 위치 | 내용 |
| --- | --- |
| `blog/` | Next.js 기술 블로그 |
| `study/challenge-120/` | 단일 Gradle 공부 프로젝트·PR 운영·장부 |
| `study/sessions/`, `study/topics/` | 학습 기록과 주제별 이해 |
| `study/redis-kotlin/` | 별도 Kotlin Redis 실습 |
| `docs/` | 조사·작업 참고 자료 |
| `output/`, `reference/` | 기존 조사 산출물과 참고 자료 |

예전 Commerce Lab과 예약 락·부하 비교 프로젝트는 2026-09-13에 종료했습니다.
블로그 글과 과거 학습 기록은 유지합니다. 종료한 프로젝트의 코드·실험 자료와 별도 복구본은 사용자 요청으로 삭제했습니다.
작업 원칙은 [AGENTS.md](AGENTS.md)를 따릅니다.
