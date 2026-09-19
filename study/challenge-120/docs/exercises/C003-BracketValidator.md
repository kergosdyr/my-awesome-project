# C003 — 괄호 문자열 검증

[구현 파일](../../coding/src/main/java/challenge/coding/BracketValidator.java) · [공개 테스트](../../coding/src/test/java/challenge/coding/BracketValidatorTest.java)

C003 개정: 괄호 문자열 검증 (구간 합 복습을 대체하는 새 문제).
boolean isValid(String text)를 구현한다. 입력은 null이 아니며 ()[]{} 문자만 포함한다.
모든 여는 괄호가 같은 종류의 닫는 괄호와 짝을 이루고, 안쪽 괄호부터 닫혀야 true.
닫는 괄호만 나타나거나 종류/중첩 순서가 틀리거나 끝까지 닫히지 않으면 false.
빈 문자열은 true. 길이 0~100_000. 입력 오류 예외 처리는 범위 밖.
예: "([]){}" -> true, "([)]" -> false, "((" -> false, "" -> true.
제한시간 Coding30분 / Minimum15분. 먼저 정확한 풀이, 이후 시간복잡도 O(n) 목표.
기본 정확성과 효율은 구분 평가. Minimum은 큰 입력 테스트 제외.
공부 목표: 순서 있는 입력에서 어떤 미완료 정보를 보관해야 하는지 판단하기.
제출: 첫 접근, 실제시간, 코드/테스트, 복잡도, 자작 반례, 도움 범위.
실행 구성 C003 - BracketValidator.
공개 테스트의 시간 제한은 로컬 실행 확인이며 O(n)의 증명은 아니다.
실행: ./gradlew :coding:test --tests '*BracketValidatorTest'
IntelliJ 구성: C003 - BracketValidator

```sh
./gradlew :coding:test --tests '*BracketValidatorTest'
```

실행 위치: `study/challenge-120`. 기존 제출 코드·학습 결과는 [장부](../../ledger.md)에 보존한다.
