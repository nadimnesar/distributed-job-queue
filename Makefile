# Makefile for managing Docker, Liquibase migrations, and build processes

# Up everything
up: build down db migrate docker-up

# Build and package the application (skipping tests)
build:
	mvn clean install -DskipTests

# Stop all running containers and remove orphaned containers
down:
	docker-compose down --remove-orphans
	docker-compose -f docker-compose-db.yml down --remove-orphans

# Start the database container
db:
	docker-compose -f docker-compose-db.yml up -d --build

# Start application containers using docker-compose
docker-up:
	docker-compose up -d --build

# Apply Liquibase database migrations to PostgreSQL
migrate:
	mvn -pl producer liquibase:update -Dliquibase.driver=org.postgresql.Driver -Dliquibase.changeLogFile=src/main/resources/db/master.json -Dliquibase.url=jdbc:postgresql://localhost:6000/job_queue_db -Dliquibase.username=postgres -Dliquibase.password=postgres

# Rollback the last Liquibase database migration
rollback:
	mvn -pl producer liquibase:rollback -Dliquibase.driver=org.postgresql.Driver -Dliquibase.changeLogFile=src/main/resources/db/master.json -Dliquibase.url=jdbc:postgresql://localhost:6000/job_queue_db -Dliquibase.username=postgres -Dliquibase.password=postgres -Dliquibase.rollbackCount=1

# Clear everyting
clear:
	@if [ -n "$$(docker ps -aq)" ]; then docker rm -f $$(docker ps -aq); fi
	@if [ -n "$$(docker images -aq)" ]; then docker rmi -f $$(docker images -aq); fi
	@if [ -n "$$(docker volume ls -q)" ]; then docker volume rm $$(docker volume ls -q); fi
	@if [ -n "$$(docker network ls --filter "type=custom" -q)" ]; then docker network rm $$(docker network ls --filter "type=custom" -q); fi
	@docker system prune -a --volumes -f
