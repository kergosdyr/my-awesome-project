package challenge.coding;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class NextHigherTest {
    final NextHigher solver = new NextHigher();
    @Test void empty() { assertArrayEquals(new int[0], solver.findIndices(new int[0])); }
    @Test void one() { assertArrayEquals(new int[]{-1}, solver.findIndices(new int[]{7})); }
    @Test void mixed() { assertArrayEquals(new int[]{3,2,3,-1,-1}, solver.findIndices(new int[]{2,1,2,4,3})); }
    @Test void equalIsNotHigher() { assertArrayEquals(new int[]{-1,-1,-1}, solver.findIndices(new int[]{4,4,4})); }
    @Test void increasing() { assertArrayEquals(new int[]{1,2,3,-1}, solver.findIndices(new int[]{1,2,3,4})); }
    @Test void decreasing() { assertArrayEquals(new int[]{-1,-1,-1}, solver.findIndices(new int[]{3,2,1})); }
    @Test void preservesInputAndReturnsNewArray() {
        int[] values = {2,3,1}, copy = values.clone(); var result = solver.findIndices(values);
        assertArrayEquals(copy, values); assertNotSame(values, result); assertArrayEquals(new int[]{1,-1,-1}, result);
    }
    @Test @Tag("full") void integerExtremes() { assertArrayEquals(new int[]{1,-1,-1}, solver.findIndices(new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE,0})); }
    @Test @Tag("full") void nearestNotMaximum() { assertArrayEquals(new int[]{1,3,3,-1}, solver.findIndices(new int[]{2,3,1,9})); }
    @Test @Tag("full") void largeInput() {
        int[] input = new int[200_000]; java.util.Arrays.fill(input, 7);
        int[] expected = new int[input.length]; java.util.Arrays.fill(expected,-1);
        assertArrayEquals(expected, solver.findIndices(input));
    }
}
