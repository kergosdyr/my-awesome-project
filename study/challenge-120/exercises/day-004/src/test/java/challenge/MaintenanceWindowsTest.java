package challenge;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceWindowsTest {
    final MaintenanceWindows subject = new MaintenanceWindows();
    void check(int[][] input, int[][] expected) {
        int[][] before = Arrays.stream(input).map(int[]::clone).toArray(int[][]::new);
        int[][] actual = subject.merge(input);
        assertTrue(Arrays.deepEquals(expected, actual), () -> "actual=" + Arrays.deepToString(actual));
        assertTrue(Arrays.deepEquals(before, input), "입력 보존");
        assertNotSame(input, actual, "반환 배열 독립");
        for (int[] row : actual) for (int[] source : input) assertNotSame(source, row, "행 공유 금지");
    }
    @Test void empty() { check(new int[][]{}, new int[][]{}); }
    @Test void singleAndNoAliasing() { check(new int[][]{{2,7}}, new int[][]{{2,7}}); }
    @Test void unsortedOverlapsAndTouching() { check(new int[][]{{8,10},{1,3},{3,6},{2,4}}, new int[][]{{1,6},{8,10}}); }
    @Test void containedAndDuplicate() { check(new int[][]{{2,5},{1,9},{1,9},{3,4}}, new int[][]{{1,9}}); }
    @Test void separated() { check(new int[][]{{9,10},{0,1},{3,5}}, new int[][]{{0,1},{3,5},{9,10}}); }
    @Test void chain() { check(new int[][]{{5,7},{1,3},{3,5}}, new int[][]{{1,7}}); }
    @Test void equalStartAndLargeEnd() { check(new int[][]{{0,4},{0,1_000_000_000}}, new int[][]{{0,1_000_000_000}}); }
    @Test @Tag("optimization") void largeReverseOrder() {
        int[][] input = new int[100_000][2];
        for (int i=0;i<input.length;i++) input[i]=new int[]{100_000-i,100_001-i};
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> check(input,new int[][]{{1,100_001}}));
    }
}
