package challenge.coding;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FirstUniqueTest {
    @ParameterizedTest
    @CsvSource(
            value = {
                "leetcode,0",
                "loveleetcode,2",
                "aabb,-1",
                "'',-1",
                "z,0",
                "aabbc,4",
                "abac,1",
                "abcabc,-1"
            })
    void publicExamples(String input, int expected) {
        assertEquals(expected, new FirstUnique().find(input));
    }

    @Test
    @Tag("full")
    void upperLengthBoundary() {
        // 정확성의 상한 입력. 시간으로 탈락시키지 않고 O(n)은 제출 코드와 설명으로도 확인한다.
        assertEquals(99_999, new FirstUnique().find("a".repeat(99_999) + "z"));
    }
}
