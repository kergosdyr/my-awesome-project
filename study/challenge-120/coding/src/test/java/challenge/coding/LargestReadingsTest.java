package challenge.coding;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class LargestReadingsTest {
    private final LargestReadings solution = new LargestReadings();

    @Test
    void mixed() {
        assertArrayEquals(new int[] {9, 7, 5}, solution.topK(new int[] {4, 9, 1, 7, 5}, 3));
    }

    @Test
    void duplicates() {
        assertArrayEquals(new int[] {8, 8, 5}, solution.topK(new int[] {8, 2, 8, 5}, 3));
    }

    @Test
    void one() {
        assertArrayEquals(new int[] {7}, solution.topK(new int[] {7}, 1));
    }

    @Test
    @Tag("full")
    void all() {
        assertArrayEquals(new int[] {5, 3, 1}, solution.topK(new int[] {1, 5, 3}, 3));
    }

    @Test
    @Tag("full")
    void extremes() {
        assertArrayEquals(new int[] {Integer.MAX_VALUE, 0}, solution.topK(new int[] {0, Integer.MAX_VALUE, 0}, 2));
    }

    @Test
    @Tag("full")
    void preserveInput() {
        int[] input = {6, 1, 4, 9};
        int[] before = input.clone();
        assertArrayEquals(new int[] {9, 6}, solution.topK(input, 2));
        assertArrayEquals(before, input);
    }

    @Test
    @Tag("full")
    void increasingAndDecreasing() {
        assertArrayEquals(new int[] {5, 4}, solution.topK(new int[] {1, 2, 3, 4, 5}, 2));
        assertArrayEquals(new int[] {5, 4}, solution.topK(new int[] {5, 4, 3, 2, 1}, 2));
    }

    @Test
    @Tag("optimization")
    void largeAgainstIndependentOracle() {
        int[] input = new Random(9).ints(200_000, 0, Integer.MAX_VALUE).toArray();
        int[] sorted = input.clone();
        Arrays.sort(sorted);
        int[] expected = new int[17];
        for (int i = 0; i < 17; i++) expected[i] = sorted[sorted.length - 1 - i];
        assertArrayEquals(expected, solution.topK(input, 17));
    }
}
