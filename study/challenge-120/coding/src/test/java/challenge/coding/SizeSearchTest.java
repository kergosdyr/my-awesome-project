package challenge.coding;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SizeSearchTest {
    private final SizeSearch search = new SizeSearch();

    @Test
    void firstDuplicate() {
        assertEquals(1, search.firstAtLeast(new int[] {90, 95, 95, 100}, 95));
    }

    @Test
    void betweenValues() {
        assertEquals(3, search.firstAtLeast(new int[] {90, 95, 95, 100}, 96));
    }

    @Test
    void none() {
        assertEquals(-1, search.firstAtLeast(new int[] {90, 95, 95, 100}, 105));
    }

    @Test
    void empty() {
        assertEquals(-1, search.firstAtLeast(new int[] {}, 95));
    }

    @Test
    void beforeFirst() {
        assertEquals(0, search.firstAtLeast(new int[] {90, 95}, 80));
    }

    @Test
    void single() {
        assertEquals(0, search.firstAtLeast(new int[] {95}, 95));
    }

    @Test
    void allSame() {
        assertEquals(0, search.firstAtLeast(new int[] {95, 95, 95}, 95));
    }

    @Test
    void integerEdges() {
        assertEquals(1, search.firstAtLeast(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}, 0));
    }

    @Test
    void preservesInput() {
        int[] input = {90, 95, 100};
        search.firstAtLeast(input, 95);
        assertArrayEquals(new int[] {90, 95, 100}, input);
    }
}
