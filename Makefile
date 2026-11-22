.PHONY: test verify run up down image k8s-dev load challenge

test:            ## unit + topology tests (fast, no broker)
	mvn -B test

verify:          ## + end-to-end test against embedded Kafka
	mvn -B verify

run:             ## run the app against localhost:9092
	mvn spring-boot:run

up:              ## Kafka + app + Kafka UI in containers
	docker compose up --build -d

down:
	docker compose down -v

image:
	docker build -t banker-platform:local .

k8s-dev:         ## render the dev overlay
	kubectl kustomize k8s/overlays/dev

load:            ## send random transactions to the running app
	python3 scripts/load_generator.py --count 200

challenge:       ## make challenge NAME=MyChallenge
	scripts/new-challenge.sh $(NAME)
