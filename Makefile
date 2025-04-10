# Makefile for managing Docker, Liquibase migrations, and build processes

# Build and start everything
up: build down data-up migrate docker-up

# Up for local development
up-local: build down data-up migrate

# Build and package the application (skipping tests)
build:
	./mvnw clean install

# Stop all running containers and remove orphaned containers
down:
	docker compose down --remove-orphans
	docker compose -f docker-compose-data.yml down --remove-orphans

# Start the database and redis container
data-up:
	docker compose -f docker-compose-data.yml up -d --build

# Apply Liquibase database migrations to Postgres
migrate:
	./mvnw -pl producer liquibase:update -Dliquibase.driver=org.postgresql.Driver -Dliquibase.changeLogFile=src/main/resources/db/master.json -Dliquibase.url=jdbc:postgresql://localhost:6000/job_queue_db -Dliquibase.username=postgres -Dliquibase.password=postgres

# Start application containers using docker-compose
docker-up:
	docker compose up -d --build

# Rollback the last Liquibase database migration to Postgres
rollback:
	./mvnw -pl producer liquibase:rollback -Dliquibase.driver=org.postgresql.Driver -Dliquibase.changeLogFile=src/main/resources/db/master.json -Dliquibase.url=jdbc:postgresql://localhost:6000/job_queue_db -Dliquibase.username=postgres -Dliquibase.password=postgres -Dliquibase.rollbackCount=1

# Clear everything
clear:
	@if [ -n "$$(docker ps -aq)" ]; then docker rm -f $$(docker ps -aq); fi
	@if [ -n "$$(docker images -aq)" ]; then docker rmi -f $$(docker images -aq); fi
	@if [ -n "$$(docker volume ls -q)" ]; then docker volume rm $$(docker volume ls -q); fi
	@if [ -n "$$(docker network ls --filter "type=custom" -q)" ]; then docker network rm $$(docker network ls --filter "type=custom" -q); fi
	@docker system prune -a --volumes -f
