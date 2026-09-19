# 재사용할 작업 정보

일반 작업 원칙과 검증 범위는 [AGENTS.md](../AGENTS.md)를 따른다.

- PDF 등 문서 작업의 런타임은 `load_workspace_dependencies`로 확인한다. 경로만으로 패키지 설치 여부를 가정하지 않는다.
- 이력서 경험 자료는 `/Users/justin/IdeaProjects/2026-resume/AI_START_HERE.md`, 기존 조사 결과는 루트의 `*resume*.csv`와 `output/`에 있다.
- 학습 상태는 `study/README.md`와 해당 주제·세션을 확인한다.
- 예전 Commerce UI·부하 비교 코드와 별도 복구본은 2026-09-13 사용자 요청으로 삭제했다. 해당 실험을 자동으로 복원하지 않는다.
- 2026-09-05 환경 정리 복구본은 `/Users/justin/.codex/skill-backups/astra-environment-2026-09-05/`에 있다.
- 로컬 k6 1000VU에서 Nginx upstream 유휴64 조건은 연결 생성 오류49(`Can't assign requested address`)를 낸 사례가 있다. 원본 실패를 보존하고 종료한 실험의 `load-comparison/run.py --jdk-idle 2048 --nginx-idle 1024`를 양쪽에 동일 적용한4회에서는 재현되지 않았다. OS 전역 튜닝 대신 테스트용 연결 재사용 조건부터 확인하며, 개별 연결 추적 없이 원인을 확정하지 않는다.
- Docker stats 스트림 수집에서 표본이 저장되지 않고 종료 대기가 실패한 경우, 이번 Testcontainer ID만 대상으로 `docker stats --no-stream --format '{{json .}}'`를 별도 스레드에서 주기 조회한다. 조회 timeout을 두고 수집 스레드를 종료한 뒤 테스트 서버를 정리한다. 부하 성공과 관측 성공은 별도로 검사한다.
- 챌린지 Gradle 실행은 `workdir`를 `study/challenge-120`의 절대 경로로 지정한다. 같은 호출에서 파일을 작성할 때도 저장소 기준 상대 경로를 중복해서 붙이지 않도록 절대 경로를 쓴다. 루트에는 Gradle Wrapper가 없다.
