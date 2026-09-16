package challenge.coding;

/**
 * C007 — 입을 수 있는 첫 사이즈. Coding 30분.
 * 작은 값부터 정렬된 sizes에서 target 이상인 첫 원소의 인덱스를 반환한다. 없으면 -1.
 * 값이 아니라 0부터 시작하는 위치이며 같은 사이즈가 여러 개면 가장 앞의 위치다.
 * <pre>
 * 입력: [90,95,95,100], target=95 → 출력: 1. 두 95 중 앞쪽 위치다.
 * 입력: [90,95,95,100], target=96 → 출력: 3. 96은 없어도 100은 조건에 맞는다.
 * 입력: [90,95,95,100], target=105 → 출력: -1. 조건에 맞는 사이즈가 없다.
 * 입력: [], target=95 → 출력: -1. 빈 목록도 허용한다.
 * </pre>
 * 입력은 null이 아니며 길이 0..200,000, 오름차순(중복 허용). 값·target은 int 전체 범위.
 * 사이즈는 상황 설명이며 음수 등 int 경계도 처리한다. 입력 변경 금지. 잘못된 입력 검증 제외.
 * 작은 입력을 손으로 풀고 정확한 기본 풀이부터 시작한다. 목표 O(log n), 추가 공간 O(1).
 * 기본 풀이 통과와 최적화 달성을 따로 평가한다. 공개 테스트 통과는 복잡도 증명이 아니다.
 * 실행: ./gradlew test --tests '*SizeSearchTest' (기존 단일 Gradle 프로젝트 터미널).
 * 제출: 초기 접근, 실제 시간, 복잡도·탐색에서 제외해도 되는 이유, 자작 반례, 도움 범위.
 * Minimum은 새 구현 대신 기존 C006 자료 없는 복습 15분으로 대체한다.
 */
public class SizeSearch {
    public int firstAtLeast(int[] sizes, int target) {
        int left = 0;
        int right = sizes.length - 1;
        int answer = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (sizes[mid] >= target) {
                answer = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return answer;
    }
}
