package challenge.coding;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ShippingCapacityTest {
    private final ShippingCapacity solution = new ShippingCapacity();

    @Test
    @Tag("minimum")
    void threeDays() {
        assertEquals(6L, solution.minimumCapacity(new int[] {1, 2, 3, 4, 5}, 3));
    }

    @Test
    @Tag("minimum")
    void oneDay() {
        assertEquals(8L, solution.minimumCapacity(new int[] {5, 1, 2}, 1));
    }

    @Test
    @Tag("minimum")
    void oneBoxPerDay() {
        assertEquals(5L, solution.minimumCapacity(new int[] {5, 1, 2}, 3));
    }

    @Test
    @Tag("full")
    void preserveOrder() {
        assertEquals(6L, solution.minimumCapacity(new int[] {3, 2, 2, 4, 1, 4}, 3));
    }

    @Test
    @Tag("full")
    void oneBox() {
        assertEquals(7L, solution.minimumCapacity(new int[] {7}, 1));
    }

    @Test
    @Tag("full")
    void exactCapacityAndUnusedDayAllowed() {
        assertEquals(4L, solution.minimumCapacity(new int[] {2, 2, 2, 2}, 3));
    }

    @Test
    @Tag("full")
    void largeSumUsesLong() {
        int[] weights = new int[200_000];
        Arrays.fill(weights, 1_000_000_000);
        assertEquals(200_000_000_000_000L, solution.minimumCapacity(weights, 1));
    }

    @Test
    @Tag("full")
    void originalArrayIsPreserved() {
        int[] weights = {9, 1, 2, 8};
        int[] before = weights.clone();
        assertEquals(10L, solution.minimumCapacity(weights, 2));
        assertArrayEquals(before, weights);
    }
}
