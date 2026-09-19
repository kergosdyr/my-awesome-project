package challenge.coding;

import java.util.PriorityQueue;

/**
 * C009: 응답 시간 중 가장 큰 k개를 큰 값부터 반환한다. 같은 값도 별개 관측이다.
 * 예: [8,2,8,5], k=3 → [8,8,5]. 입력을 바꾸지 않는다.
 * 길이 1~200,000, 값 0~Integer.MAX_VALUE, 1<=k<=길이. 잘못된 입력은 제외.
 * 기본: 정확한 결과. 최적화 목표: O(n log(k+1)+k log k), 추가 공간 O(k).
 * 실행: ./gradlew :coding:test --tests '*LargestReadingsTest'
 */
public class LargestReadings {
    public int[] topK(int[] readings, int k) {
        PriorityQueue<Integer> candidates = new PriorityQueue<>();
        for (int i = 0; i < readings.length; i++) {
            candidates.offer(readings[i]);

            if (candidates.size() > k) {
                candidates.poll();
            }
        }

        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i++) {
            result[i] = candidates.poll();
        }
        return result;
    }
}
