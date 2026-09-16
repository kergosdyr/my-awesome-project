package challenge.coding;

/**
 * Day 001 · C001 — 처리 예산 안의 가장 긴 연속 구간 (25분)
 *
 * <h2>문제</h2>
 * 로그별 처리 비용 costs가 원래 순서대로 주어진다.
 * 비용 합이 budget 이하인 연속 구간 중 최대 길이를 반환한다.
 * 가능한 비어 있지 않은 구간이 없으면 0을 반환한다.
 *
 * <h2>제약조건</h2>
 * <ul>
 *   <li>costs는 null이 아니며 길이는 0~200,000.</li>
 *   <li>각 비용은 0~1,000,000,000.</li>
 *   <li>budget은 0~1,000,000,000,000,000.</li>
 *   <li>입력 배열을 변경하지 않는다.</li>
 *   <li>목표: 시간 O(n), 추가 공간 O(1).</li>
 * </ul>
 *
 * <h2>예시</h2>
 * <pre>
 * costs = [4, 2, 1, 7, 3], budget = 8  → 3
 * costs = [0, 0, 5],       budget = 0  → 2
 * costs = [],             budget = 10 → 0
 * </pre>
 *
 * <h2>실행 및 제출</h2>
 * IntelliJ 상단에서 {@code C001 - LongestBatch}을 선택하고 ▶ 실행.
 * 초기 접근, 구현, 본인이 추가한 경계 테스트와 결과, 시간·공간 복잡도를 제출한다.
 * 목표 복잡도에 도달하지 못해도 현재 풀이와 한계를 그대로 남긴다.
 * 공개 테스트 통과만으로 복잡도와 모든 경우의 정당성이 증명되지는 않는다.
 * 초기 벤치마크에서는 AI 풀이 없이 시도했다. 현재 코드는 사용자 요청으로 제공한 해설 풀이이며
 * 독립 해결 결과로 평가하지 않는다. 초기 시도는 attempts/에 보존했다.
 * 제공된 구현 틀과 테스트는 독립성 감점에서 제외한다.
 * 실행: ./gradlew :coding:test --tests '*LongestBatchTest'
 * IntelliJ 구성: C001 - LongestBatch
 */
public class LongestBatch {
    /**
     * 사용자 요청으로 제공한 해설 풀이: 슬라이딩 윈도우.
     * left와 right는 현재 검사하는 연속 구간의 양 끝이다.
     * 모든 비용이 0 이상이므로, 오른쪽을 늘리면 합이 줄지 않고
     * 왼쪽을 제거하면 합이 늘지 않는 성질을 이용한다.
     */
    public int longestBatch(int[] costs, long budget) {
        int left = 0;
        int maxLength = 0;
        long windowSum = 0; // 여러 int 비용의 합은 int 범위를 넘을 수 있다.

        for (int right = 0; right < costs.length; right++) {
            // 새 원소를 현재 구간의 오른쪽에 포함한다.
            windowSum += costs[right];

            // 예산을 넘으면 구간 전체를 버리지 않고, 왼쪽부터 하나씩 제외한다.
            // 하나를 제외해도 초과할 수 있으므로 if가 아니라 while이다.
            while (windowSum > budget) {
                windowSum -= costs[left];
                left++;
            }

            // 여기서는 항상 windowSum <= budget이다.
            // 현재 원소 하나도 예산을 넘으면 그 원소까지 제거되어
            // left == right + 1, 즉 구간 길이는 0이 된다.
            int currentLength = right - left + 1;
            maxLength = Math.max(maxLength, currentLength);
        }

        // right는 n번, left도 전체 실행 동안 최대 n번만 앞으로 이동한다.
        // 중첩 반복문이어도 총 이동은 최대 2n번이므로 시간 O(n).
        // 입력 크기와 무관한 변수만 사용하므로 추가 공간 O(1).
        return maxLength;
    }
}
