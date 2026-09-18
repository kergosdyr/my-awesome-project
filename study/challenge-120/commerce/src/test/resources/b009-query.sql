-- B009: 사용자 요청으로 제공하는 주문 범위 조회 SQL. 인덱스만 직접 설계한다.
-- 품목/결제 JOIN 비용은 제외하고 주문의 검색·정렬 비용을 관찰한다.
-- size=20에 다음 페이지 확인용 1개를 더 읽는다.

-- 1. 첫 페이지: 아직 읽은 주문이 없다.
SELECT id, created_at
FROM store_order
ORDER BY created_at DESC, id DESC
LIMIT 21;

-- 2. 이어 조회: 도구가 18,000번째 주문의 시각과 ID를 아래 값에 넣는다.
SELECT id, created_at
FROM store_order
WHERE created_at < '${boundaryTime}'
   OR (created_at = '${boundaryTime}' AND id < ${boundaryId})
ORDER BY created_at DESC, id DESC
LIMIT 21;

-- 인덱스 적용 전후에는 같은 SELECT끼리 비교한다.
-- 도구의 OFFSET 기준 쿼리는 20개, 위 쿼리는 21개이므로 시간 비율을 그대로 개선율로 쓰지 않는다.
