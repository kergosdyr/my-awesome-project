package challenge.coding;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * C005 — 다음으로 더 높은 측정값. 제한 30분(Minimum 15분).
 * 각 위치 i에서 오른쪽으로 처음 만나는, values[i]보다 엄격히 큰 값의 인덱스를 반환한다.
 * 그런 위치가 없으면 -1. 같은 값은 답이 아니다. 입력을 변경하지 않고 새 int[]를 반환한다.
 * 인덱스는 0부터 시작하며 값 자체가 아니라 위치를 반환한다.
 * <pre>
 * 예제 1
 * 입력: values = [2,1,2,4,3]
 * 출력: [3,2,3,-1,-1]
 * 설명: 위치 0의 2보다 큰 첫 값은 위치 3의 4다. 위치 2의 2는 같은 값이므로 제외한다.
 *       위치 1의 1은 위치 2의 2를, 위치 2의 2는 위치 3의 4를 만난다.
 *       위치 3과 4에는 오른쪽에 더 큰 값이 없다.
 *
 * 예제 2
 * 입력: values = [2,3,1,9]
 * 출력: [1,3,3,-1]
 * 설명: 위치 0의 답은 가장 큰 값 9의 위치 3이 아니라 처음 만나는 3의 위치 1이다.
 *       위치 1과 2의 답은 모두 9의 위치 3이다. 마지막 위치의 답은 -1이다.
 *
 * 예제 3
 * 입력: values = [4,4,4]
 * 출력: [-1,-1,-1]
 * 설명: 같은 값은 엄격히 큰 값이 아니므로 모든 위치에 답이 없다.
 * </pre>
 * 빈 입력 []의 출력은 []다.
 * 입력은 null이 아니며 길이 0..200,000, 원소는 int 전체 범위. 잘못된 입력 검증은 범위 밖.
 * 작은 입력을 손으로 풀고 느려도 정확한 구현부터 시도한다. 목표 O(n), 기본 정확성과 별도 평가.
 * 실행 구성: Day 005 - Coding / Day 005 - Coding Minimum.
 * 제출: 초기 접근, 실제 시간, 코드, 복잡도와 안전한 이유, 자작 반례, 도움 범위.
 * 공개 테스트는 기능 표본이며 시간복잡도를 증명하지 않는다. 공개 경계는 독립 발견으로 세지 않는다.
 */
public class NextHigher {
    public int[] findIndices(int[] values) {
        // AI 해답: 2026-09-13 사용자 요청으로 제공. 독립 구현과 구분한다.
        int[] answer = new int[values.length];
        Arrays.fill(answer, -1); // 끝까지 답을 못 찾은 위치는 -1을 유지한다.

        ArrayDeque<Integer> waitings = new ArrayDeque<>();

        for (int i = 0; i < values.length; i++) {

            while (!waitings.isEmpty() && (values[waitings.peekLast()] < values[i])) {
                answer[waitings.removeLast()] = i;
            }
            waitings.add(i);
        }

        return answer;
    }
}
