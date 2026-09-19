package challenge.coding;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * C004: 여러 점검 시간 구간을 겹치지 않는 구간으로 합친다. 새 문제이며 구간 합 복습이 아니다.
 * 입력 int[][] windows: 각 행 [start, end], 0 <= start < end <= 1_000_000_000.
 * 행 개수 0~100_000, null/잘못된 행은 입력되지 않는다. 입력 순서는 임의다.
 * 겹치거나 끝과 시작이 같은 구간은 합친다. 결과는 시작 시각 오름차순이며 겹치거나 맞닿지 않는다.
 * 예: [[8,10],[1,3],[3,6],[2,4]] -> [[1,6],[8,10]], [] -> [].
 * 입력 배열과 각 행을 변경하지 않는다. 반환 배열의 행도 입력 행과 공유하지 않는다.
 * 시간: 기본30분 / Minimum15분. 작은 입력의 정확한 풀이부터 작성한다.
 * 기본 정확성과 효율을 별도 평가한다. 기본 목표 O(n log n), Minimum은 큰 입력 제외.
 * 학습: 구간의 포함/겹침/접촉 구분, 결과 순서와 불변조건, 입력 보존, 복잡도 설명.
 * 제출: 초기 접근, 실제 시간, 코드, 자작 반례, 복잡도, 자료/AI 도움 범위.
 * 공개 성능 테스트 통과만으로 점근 복잡도가 증명되지는 않는다.
 * 실행: ./gradlew :coding:test --tests '*MaintenanceWindowsTest'
 * IntelliJ 구성: C004 - MaintenanceWindows
 */
public class MaintenanceWindows {
    public int[][] merge(int[][] windows) {
        if (windows.length == 0) {
            return new int[0][];
        }

        int[][] sorted = new int[windows.length][];
        for (int i = 0; i < windows.length; i++) {
            sorted[i] = windows[i].clone();
        }

        Arrays.sort(sorted, Comparator.comparingInt(interval -> interval[0]));

        ArrayList<int[]> result = new ArrayList<>();

        int start = sorted[0][0];
        int end = sorted[0][1];

        for (int i = 0; i < sorted.length; i++) {
            int nextStart = sorted[i][0];
            int nextEnd = sorted[i][1];
            if (nextEnd <= end) {
                end = Math.max(end, nextEnd);
            } else {
                result.add(new int[] {start, end});
                start = nextStart;
                end = nextEnd;
            }
        }

        result.add(new int[] {start, end});
        return result.toArray(new int[0][]);

    }
}
