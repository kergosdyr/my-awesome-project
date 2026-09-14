package challenge.coding;

import java.util.concurrent.atomic.AtomicLong;

/**
 * C002 — 허용 지연 안의 최장 관측 구간. 제한시간 30분(Minimum 15분).
 * 시간순 관측값 delays에서 연속한 원소들의 합이 allowance 이하인 구간의 최대 길이를 반환한다.
 * 원소를 건너뛸 수 없다. 가능한 비어 있지 않은 구간이 없으면 0. 입력 배열은 변경하지 않는다.
 * 입력: null이 아닌 int[] delays, long allowance. 길이 0..200,000,
 * 각 값 0..1,000,000,000, allowance 0..200,000,000,000,000. 잘못된 입력 검사는 범위 밖.
 * 예: [3,1,2,4,1], 6 -> 3; [8,1,1,1], 3 -> 3; [], 0 -> 0.
 * 반환: int 최대 길이. 예외를 정상 결과로 반환하지 않는다.
 * 작은 입력을 손으로 풀기 → 느려도 정확한 기본 풀이 → 중복 계산 찾기 → 최적화 순으로 진행한다.
 * 처음부터 O(n)을 떠올릴 필요는 없다. 기본 정확성과 큰 입력 효율은 별도로 평가한다.
 * 실행 구성: reservation-cancellation - Coding / reservation-cancellation - Coding Minimum.
 * 명령: ./gradlew :day-002:test --tests challenge.LongestMonitoringSpanTest --rerun-tasks
 * 제출: 초기 접근, 코드, 직접 만든 반례 하나, 시간·공간 복잡도와 종료 근거, 도움 내역.
 * 공개 테스트 통과만으로 전체 정당성이나 독립 해결을 확정하지 않는다.
 */
public class LongestMonitoringSpan {
    public static int longest(int[] delays, long allowance) {
        int best = 0;
        int left = 0;
        long sum = 0;
        for (int right = 0; right < delays.length; right++) {
            sum += delays[right];

            while (sum > allowance) {
                sum -= delays[left];
                left ++;
            }
            int len = right - left + 1;
            best = Math.max(best, len);
        }

        return best;
    }
}
