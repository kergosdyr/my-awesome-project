package challenge.coding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
class BracketValidatorTest {
    private final BracketValidator solution=new BracketValidator();
    @Test void empty() { assertTrue(solution.isValid("")); }
    @Test void adjacentPairs() { assertTrue(solution.isValid("()[]{}")); }
    @Test void nestedAndAdjacent() { assertTrue(solution.isValid("([]){}")); }
    @Test void crossingPairs() { assertFalse(solution.isValid("([)]")); }
    @Test void wrongType() { assertFalse(solution.isValid("(]")); }
    @Test void unexpectedClosing() { assertFalse(solution.isValid(")(")); }
    @Test void unclosed() { assertFalse(solution.isValid("((")); }
    @Test void validPrefixThenExtraClosing() { assertFalse(solution.isValid("()[]}")); }
    @Test @Tag("optimization") void largeNesting() {
        assertTimeoutPreemptively(Duration.ofSeconds(2),()->
            assertTrue(solution.isValid("(".repeat(50_000)+")".repeat(50_000))));
    }
}
