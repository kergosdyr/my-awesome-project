# Review queue

이 파일은 활성 복습 주제의 인덱스다. 실제 일정과 결과의 기준은 각 주제 노트의 `next_review`, `interval_days`, `review_status` frontmatter다. 매일 체크인 때 연결된 주제 노트를 읽고 `next_review`가 오늘이거나 이미 지난 항목 중 가장 오래 밀린 것부터 하나만 시작한다.

| 주제 | 주제 노트 |
| --- | --- |
| AI를 회상 튜터로 사용하는 학습 루프 | [topic](topics/ai-retrieval-tutor.md) |
| Shopify 재고 예약: Redis에서 MySQL로 | [topic](topics/shopify-inventory-redis-to-mysql.md) |

## 결과 표기

- `scheduled`: 다음 복습을 기다림
- `in_progress`: 회상 질문을 시작함
- `paused`: 사용자가 의도적으로 미룸
- `mastered`: 30일 간격 복습까지 안정적으로 마침
