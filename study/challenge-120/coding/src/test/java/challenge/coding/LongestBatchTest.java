package challenge.coding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LongestBatchTest {
    private final LongestBatch solution = new LongestBatch();

    @Test void statementExample() { assertEquals(3, solution.longestBatch(new int[]{4,2,1,7,3}, 8)); }
    @Test void zeroBudget() { assertEquals(2, solution.longestBatch(new int[]{0,0,5}, 0)); }
    @Test void emptyInput() { assertEquals(0, solution.longestBatch(new int[]{}, 10)); }
    @Test void nothingFits() { assertEquals(0, solution.longestBatch(new int[]{9,10}, 8)); }
    @Test void exactBudgetAndWholeArray() { assertEquals(3, solution.longestBatch(new int[]{2,3,4}, 9)); }
    @Test void bestIntervalCanBeAtEnd() { assertEquals(3, solution.longestBatch(new int[]{9,1,1,1}, 3)); }
    @Test void sumExceedsIntegerRange() {
        assertEquals(3, solution.longestBatch(new int[]{1_000_000_000,1_000_000_000,1_000_000_000}, 3_000_000_000L));
    }
    @Test void inputIsPreserved() {
        int[] input = {4,2,1,7,3};
        int[] original = input.clone();
        assertEquals(3, solution.longestBatch(input, 8));
        assertArrayEquals(original, input);
    }
    // 튜터 검증용 반례: 사용자가 작성한 테스트와 구분합니다.
    @Test void combinedCostMustFitBudget() {
        assertEquals(1, solution.longestBatch(new int[]{2, 2, 2, 9}, 3));
    }
    @Test void singleFittingElementAtEnd() {
        assertEquals(1, solution.longestBatch(new int[]{1}, 1));
    }
    // 튜터 3차 검증: 새 구현의 구간 재시작과 누적값 범위 확인.
    @Test void bestIntervalOverlapsBudgetExceededPrefix() {
        assertEquals(3, solution.longestBatch(new int[]{4, 2, 1, 1}, 4));
    }
    @Test void accumulatedCostMustNotWrapAround() {
        assertEquals(2, solution.longestBatch(
            new int[]{1_000_000_000, 1_000_000_000, 1_000_000_000}, 2_000_000_000L));
    }
    // TODO: 본인이 추가로 생각한 경계 테스트를 여기에 작성하세요.
}
