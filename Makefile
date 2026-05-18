.PHONY: dev build up down logs migrate test clean

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
	docker compose exec api-service alembic upgrade head

test:
	docker compose exec api-service pytest

shell:
	docker compose exec api-service bash

clean:
	docker compose down -v
	docker system prune -f
