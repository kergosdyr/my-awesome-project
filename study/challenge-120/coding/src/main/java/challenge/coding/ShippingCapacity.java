package challenge.coding;

/**
 * C008 — 약속한 날짜 안에 택배 보내기. Coding 30분.
 * 상자를 주어진 순서대로 보내며 한 상자를 나누거나 순서를 바꾸지 않는다.
 * 매일 같은 무게 한도를 쓰고 days일 이내에 모두 보내는 최소 한도를 반환한다.
 * [1,2,3,4,5], days=3 → 6: [1,2,3] / [4] / [5].
 * [3,2,2,4,1,4], days=3 → 6: [3,2] / [2,4] / [1,4].
 * [5,1,2], days=1 → 8. [5,1,2], days=3 → 5.
 * 입력: 길이1..200,000, 각 무게1..1,000,000,000, days1..길이. 유효 입력만 제공.
 * 합계는 int 범위를 넘을 수 있다. 입력 배열 변경 금지.
 * 목표: 무게 합을 S라 할 때 O(n log S) 시간, O(1) 추가 공간. 통과와 복잡도 설명은 따로 평가.
 * 해답 설명 후 사용자 구현. 후보 한도로 필요한 날짜 수를 계산하고 가능한 최소 한도를 찾는다.
 * 실행: ./gradlew :coding:test --tests '*ShippingCapacityTest'
 */
public class ShippingCapacity {
    public long minimumCapacity(int[] weights, int days) {
        long max = 0;
        long sum = 0;
        for (int i = 0; i < weights.length; i++) {
            max = Math.max(weights[i], max);
            sum += weights[i];
        }

        long left = max;
        long right = sum;
        while (left < right) {
            long mid = left + (right - left) / 2;
            if (canShip(weights, days, mid)) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }

    public boolean canShip(int[] weights, int days, long capacity) {
        int usedDays = 1;
        long loaded = 0;

        for (int i = 0; i < weights.length; i++) {
            int weight = weights[i];
            if (loaded + weight > capacity) {
                usedDays++;
                loaded = 0;
            }
            loaded += weight;
            if (usedDays > days) {
                return false;
            }
        }

        return true;
    }
}
