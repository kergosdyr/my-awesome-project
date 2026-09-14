package challenge.coding;
import java.util.Arrays;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
@Timeout(10)
class LongestMonitoringSpanTest {
    @Test void example() { assertEquals(3, LongestMonitoringSpan.longest(new int[]{3,1,2,4,1},6)); }
    @Test void suffixAfterOversizedValue() { assertEquals(3, LongestMonitoringSpan.longest(new int[]{8,1,1,1},3)); }
    @Test void empty() { assertEquals(0, LongestMonitoringSpan.longest(new int[]{},0)); }
    @Test void noValidElement() { assertEquals(0, LongestMonitoringSpan.longest(new int[]{7},6)); }
    @Test void zeroBudgetAndZeros() { assertEquals(3, LongestMonitoringSpan.longest(new int[]{0,0,1,0,0,0},0)); }
    @Test void entireInput() { assertEquals(4, LongestMonitoringSpan.longest(new int[]{2,0,3,1},6)); }
    @Test void preservesInput() { int[] a={4,2,1,3}; int[] b=a.clone(); assertEquals(3,LongestMonitoringSpan.longest(a,6)); assertArrayEquals(b,a); }
    @Test void sumExceedsIntRange() { assertEquals(3,LongestMonitoringSpan.longest(new int[]{1000000000,1000000000,1000000000},3000000000L)); }
    @Test @Tag("optimization") void largeInput() { int[] a=new int[200000]; Arrays.fill(a,1); assertEquals(100000,LongestMonitoringSpan.longest(a,100000)); }
}
