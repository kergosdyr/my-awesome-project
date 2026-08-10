.PHONY: help up down logs test test-backend test-frontend

help:
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*?##/ {printf "%-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

up: ## 전체 스택을 빌드하고 백그라운드로 실행합니다.
	docker compose up --build --detach

down: ## 컨테이너를 중지합니다. MySQL 데이터는 보존됩니다.
	docker compose down

logs: ## 전체 서비스 로그를 따라갑니다.
	docker compose logs --follow

test: test-backend test-frontend ## 백엔드와 프론트엔드 검증을 모두 실행합니다.

test-backend: ## 백엔드 테스트를 실행합니다.
	cd backend && ./gradlew --no-daemon build

test-frontend: ## 프론트엔드 lint, typecheck, test, build를 실행합니다.
	cd frontend && npm ci && npm run lint && npm run typecheck && npm run test && npm run build
