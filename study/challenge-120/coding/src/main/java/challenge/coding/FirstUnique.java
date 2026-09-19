package challenge.coding;

/**
 * C006 — 처음 한 번만 등장한 문자. 정답/풀이 패턴은 시도 후에 요청한다. 오늘 해결할 문제: 문자열 전체에서 정확히 한 번 등장한 문자 중 가장 앞선 인덱스를
 * 반환한다. 왜 중요한가: 앞에서 처음 본 문자도 뒤에서 반복될 수 있어 전체 입력 기준 판단이 필요하다. 핵심 개념: 전체 등장 횟수(입력 전체 기준), 인덱스(0부터),
 * 시간/추가 공간 비용. 완료 기준: 기본 공개 테스트, 복잡도 설명, 자작 반례. 최적화는 별도 확인한다.
 *
 * <p>입력: null이 아닌 영문 소문자 a~z 문자열, 길이 0~100,000. 출력: 조건을 만족하는 최소 인덱스, 없으면 -1. 입력 변경 없음. 예1:
 * "leetcode" -> 0. l은 한 번 등장하며 가장 앞이다. 예2: "loveleetcode" -> 2. l과 o는 반복되고 v가 첫 유일 문자다. 예3: "aabb"
 * -> -1. 모든 문자가 반복된다. 예4: "" -> -1. 후보가 없다. 작은 입력 수작업 -> 느려도 정확한 구현 -> 반복 작업 찾기 -> 최적화 순서. 기본 정확성과
 * O(n) 최적화 목표를 구분 평가한다. 시간 초과만으로 정답을 공개하지 않는다.
 * 실행: ./gradlew :coding:test --tests '*FirstUniqueTest'
 * IntelliJ 구성: C006 - FirstUnique
 */
public class FirstUnique {
    public int find(String text) {
        int[] counts = new int[26];

        for (int i = 0; i < text.length(); i++) {
            counts[text.charAt(i) - 'a']++;
        }

        for (int i = 0; i < text.length(); i++) {
            if (counts[text.charAt(i) - 'a'] == 1) {
                return i;
            }
        }
        return -1;
    }
}
