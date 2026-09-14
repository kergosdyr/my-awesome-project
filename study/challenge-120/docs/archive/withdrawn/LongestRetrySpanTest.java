package challenge;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LongestRetrySpanTest {
    private final LongestRetrySpan solution=new LongestRetrySpan();
    @Test void empty() { assertEquals(0,solution.solve(new int[]{},4)); }
    @Test void example() { assertEquals(2,solution.solve(new int[]{2,1,3,1},4)); }
    @Test void zeros() { assertEquals(3,solution.solve(new int[]{0,0,0},0)); }
    @Test void none() { assertEquals(0,solution.solve(new int[]{5,6},4)); }
    @Test void endingAtLastElement() { assertEquals(3,solution.solve(new int[]{8,1,1,1},3)); }
    @Test void largeSumAndNoMutation() {
        int[] a={1_000_000_000,1_000_000_000,1_000_000_000}; int[] before=a.clone();
        assertEquals(3,solution.solve(a,3_000_000_000L)); assertArrayEquals(before,a);
    }
}
