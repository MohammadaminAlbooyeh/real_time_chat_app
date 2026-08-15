.PHONY: dev build up down logs migrate test test-all shell clean

dev:
	docker compose -f docker-compose.yml -f docker-compose.dev.yml up

build:
	docker compose build

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

migrate:
	docker compose exec api-service java -jar app.jar -- migrate

test-api:
	cd services/api-service && mvn test

test-search:
	cd services/search-service && mvn test

test-worker:
	cd services/worker-service && mvn test

test-ws:
	cd services/websocket-service && mvn test

test-all: test-api test-search test-worker test-ws

shell:
	docker compose exec api-service sh

clean:
	docker compose down -v
	docker system prune -f
