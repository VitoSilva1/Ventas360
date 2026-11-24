COMPOSE = docker compose -f docker-compose.yml -f docker-compose.kafka.yml
KAFKA_COMPOSE = docker compose -f docker-compose.kafka.yml

up:
	@[ -f .env ] || cp .env.example .env
	$(COMPOSE) up --build -d

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f

kafka-up:
	$(KAFKA_COMPOSE) up -d

kafka-down:
	$(KAFKA_COMPOSE) down

kafka-topics:
	docker exec ventas360-kafka-1 kafka-topics --bootstrap-server kafka-1:29092 --describe --topics-with-overrides

.PHONY: up down logs kafka-up kafka-down kafka-topics
