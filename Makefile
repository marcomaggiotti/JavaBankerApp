.PHONY: test verify run up down image k8s-dev load challenge

# Host ports, overridable: make up APP_PORT=28080   (same names as in .env / docker-compose.yml)
APP_PORT ?= 18080
KAFKA_PORT ?= 19092
SERVER_PORT ?= 8080

test:            ## unit + topology tests (fast, no broker)
	mvn -B test

verify:          ## + end-to-end test against embedded Kafka
	mvn -B verify

run:             ## run the app outside Docker against the compose Kafka (make run SERVER_PORT=28080)
	SERVER_PORT=$(SERVER_PORT) KAFKA_BOOTSTRAP_SERVERS=localhost:$(KAFKA_PORT) mvn spring-boot:run

up:              ## Kafka + app + Kafka UI + load generator in containers
	APP_PORT=$(APP_PORT) KAFKA_PORT=$(KAFKA_PORT) docker compose up --build -d

down:
	docker compose down -v

image:
	docker build -t banker-platform:local .

k8s-dev:         ## render the dev overlay
	kubectl kustomize k8s/overlays/dev

load:            ## send random transactions to the app started with `make up`
	python3 scripts/load_generator.py --count 200 --url http://localhost:$(APP_PORT)

challenge:       ## make challenge NAME=MyChallenge
	scripts/new-challenge.sh $(NAME)
