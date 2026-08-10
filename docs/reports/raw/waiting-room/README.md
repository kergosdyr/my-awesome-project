# Waiting-room raw evidence

이 디렉터리는 Redis FIFO waiting-room 실험의 원시 k6 summary, backend metrics,
환경 정보와 실패 정책 재현을 보존한다. 최종 비교는 각 run의 집계값을 다시
합치지 않고, 동일 구현에서 완주한 3개 run의 **run-level 중앙값**을 사용한다.

## 최종 비교에 포함

- `waiting-room-direct-run{1,2,3}-0e9dcb1/`
- `waiting-room-queued-run{1,2,3}-0e9dcb1/`

여섯 디렉터리는 commit `0e9dcb17eb3706878f102e3920936e74297bd87a`에서
완주했으며 각 디렉터리에 `summary.json`, 사람이 읽는 `summary.txt`,
`backend-metrics.json`, `backend-prometheus.txt`, `environment.txt`, `reset.json`이
있다. 세 queued run 모두 7,574 iteration을 완료했고 errors와
`dropped_iterations`가 0이다.

각 `environment.txt`는 commit과 host/tool 버전을 보존하지만
`[compose-services]` 출력이 비어 있다. 따라서 이 raw만으로는 실행 시점의
container CPU/memory 제한 적용 여부를 독립 입증할 수 없다. public 저장소에
불필요한 로컬 식별자를 남기지 않도록 hostname만 `[redacted-hostname]`으로
치환했고 OS, CPU, memory와 tool version은 유지했다.

중앙값은 `summary.json`의 다음 필드에서 독립 재계산했다.

- accepted/rejected: `waiting_room_accepted_total`, `waiting_room_rejected_total`
- 성공 end-to-end: `waiting_room_accepted_end_to_end_duration`
- 대기 시간: `waiting_room_wait_duration`
- 전체 HTTP 요청: `http_reqs`
- peak VU: `vus.max`
- queue/active/FIFO: k6 backend gauge와 `backend-metrics.json`을 상호 대조

`backend-prometheus.txt`는 lab reset과 무관한 **JVM 생애 누적 snapshot**이라
개별 run 수치나 중앙값 계산에 사용하지 않았다. run별 결론은 `summary.json`과
lab namespace가 reset되는 `backend-metrics.json`만 근거로 한다.

`http_reqs`는 ticket 발급, poll, 구매, metrics sampling을 모두 합친 값이다.
따라서 final queued 중앙값 191,071과 아래 preflight의 473,624를 정확한 poll
요청 수로 표현하면 안 된다.

## 최종 실패 정책 검증

- `failure-redis-unavailable-0e9dcb1/`: Redis 중단 시 ticket/metrics는 503,
  Redis를 사용하지 않는 direct 구매는 200.
- `failure-token-policy-0e9dcb1/`: 정상 token은 200, 위조 token은 403,
  소비된 token replay는 410.
- `failure-admission-expiry-0e9dcb1/`: 정확히 4 slot을 채운 뒤 5번째 poll은
  202, admission TTL 10초가 지난 뒤 재-poll은 200, 첫 만료 token 구매는
  410. 각 호출의 trace/body/status와 전후 metrics를 함께 보존한다.

## 제외한 preflight

- `waiting-room-*-14bc9ed/`: strict head-only 100ms polling 시기의 탐색 실행.
  queued run 1은 중단됐고 1,531 dropped iteration이 있으며, queued run 2는
  완료 iteration이 0이다. run 1의 473,624는 **전체 HTTP 요청 수**이지
  poll-only count가 아니다.
- `waiting-room-*-c6d1237/`: batch pre-promotion 구현의 탐색 실행. queued run 1은
  중단됐고 33 dropped iteration이 있으며 2,467 iteration만 완료했다.
- `failure-admission-expiry-invalid-preflight-0e9dcb1/`: admission을 2개만
  활성화한 상태에서 수행해 4-slot capacity expiry를 검증하지 못한 원본이다.
  감사 가능성을 위해 삭제하지 않고 이름만 바꿔 보존했다.

위 preflight는 설계 실패 원인을 설명하는 증거로만 사용하며 최종 중앙값이나
direct/queued 개선율 계산에는 포함하지 않는다.
