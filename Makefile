.PHONY: help setup-blog test check check-study check-blog verify build-blog

help:
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*?##/ {printf "%-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

setup-blog: ## 최초 설정 또는 blog lockfile 변경 때 의존성을 설치합니다.
	cd blog && npm ci

test: ## 현재 Day5 코딩 테스트를 실행합니다.
	cd study/challenge-120 && ./gradlew :day-005:test --tests 'challenge.coding.*'

check-study: ## 전체 실습 컴파일과 Day5 코딩·제공 인프라를 확인합니다. 업무 TODO 검사는 각 과제에서 실행합니다.
	cd study/challenge-120 && ./gradlew testClasses :day-005:test --tests 'challenge.coding.*' --tests '*PaymentInfrastructureTest'

check-blog: ## 블로그 lint와 typecheck를 실행합니다.
	cd blog && npm run lint && npm run typecheck

check: check-study check-blog ## 공부 실습 환경과 블로그를 검사합니다.

verify: check ## 검사 후 블로그 배포 빌드를 만듭니다.
	$(MAKE) build-blog

build-blog: ## 블로그 배포 빌드를 만듭니다. 게시하지 않습니다.
	cd blog && npm run build
