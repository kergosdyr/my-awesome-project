.PHONY: help setup-blog test check check-study check-blog verify build-blog

help:
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*?##/ {printf "%-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

setup-blog: ## 최초 설정 또는 blog lockfile 변경 때 의존성을 설치합니다.
	cd blog && npm ci

test: ## coding 모듈의 전체 코딩 테스트를 실행합니다.
	cd study/challenge-120 && ./gradlew :coding:test

check-study: ## 두 모듈 컴파일·완료 코딩28개·활성 커머스43개 회귀 검사를 실행합니다.
	cd study/challenge-120 && ./gradlew :coding:testClasses :commerce:testClasses ciTest

check-blog: ## 블로그 lint와 typecheck를 실행합니다.
	cd blog && npm run lint && npm run typecheck

check: check-study check-blog ## 공부 실습 환경과 블로그를 검사합니다.

verify: check ## 검사 후 블로그 배포 빌드를 만듭니다.
	$(MAKE) build-blog

build-blog: ## 블로그 배포 빌드를 만듭니다. 게시하지 않습니다.
	cd blog && npm run build
