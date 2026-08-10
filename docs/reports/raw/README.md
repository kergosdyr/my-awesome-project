# Raw load-test evidence

이 디렉터리는 통합 [부하 테스트 보고서](../load-test-report.md)의 원본 증거다. 대표값은 요청을 합치지 않고, 같은 비교군의 독립 실행 3개에서 계산한 run-level 중앙값이다.

- [`cache-stampede/`](cache-stampede/): main 맥락 측정, naive/protected 각 3회, Redis/lock 실패 정책
- [`kafka-outbox/`](kafka-outbox/): direct/outbox 각 3회, broker 중단·복구·중복 처리
- [`waiting-room/`](waiting-room/): final direct/queued 각 3회, token/Redis/lease 실패 정책, 제외한 preflight

일반적인 run 디렉터리에는 다음 파일이 있다.

```text
summary.json             k6 handleSummary 원본
summary.txt              사람이 읽는 동일 summary
environment.txt          commit, OS, CPU, Java, k6, Docker 환경
backend-metrics.json     resettable lab correctness counter
backend-prometheus.txt   실행 종료 시 Prometheus snapshot
reset.json               실행 직전 fixture와 policy 상태
```

Prometheus snapshot은 프로세스 누적값일 수 있다. Run-level HTTP 수와 latency는 `summary.json`, lab 정확성 counter는 실행 직전 reset한 `backend-metrics.json`을 기준으로 한다. 중단되거나 불완전한 실행은 삭제하지 않되 해당 lab의 README에서 계산 제외 사유를 명시한다.
