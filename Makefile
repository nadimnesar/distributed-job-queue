# Makefile for managing Docker, Liquibase migrations, and build processes

# Main commands for system initialization and updates
# -----------------------------------------------------

# Start everything: build the application, set up database, apply migrations, and start Docker containers
up: build db db-up docker-up

# Update the system: rebuild application, stop containers, reset database, apply migrations, and restart containers
update: build down db db-up docker-up

# Stop all running containers and remove orphaned containers
down:
	docker-compose down --remove-orphans
	docker-compose -f docker-compose-db.yml down --remove-orphans

# Build and package the application (skipping tests)
build:
	mvn clean install -DskipTests

# Start application containers using docker-compose
docker-up:
	docker-compose up -d --build

# Database management commands
# -----------------------------------------------------

# Start the database container
db:
	docker-compose -f docker-compose-db.yml up -d --build

# Apply Liquibase database migrations to PostgreSQL
db-up:
	mvn -pl producer liquibase:update -Dliquibase.driver=org.postgresql.Driver -Dliquibase.changeLogFile=src/main/resources/db/master.json -Dliquibase.url=jdbc:postgresql://localhost:6000/job_queue_db -Dliquibase.username=postgres -Dliquibase.password=postgres

# Rollback the last Liquibase database migration
db-down:
	mvn -pl producer liquibase:rollback -Dliquibase.driver=org.postgresql.Driver -Dliquibase.changeLogFile=src/main/resources/db/master.json -Dliquibase.url=jdbc:postgresql://localhost:6000/job_queue_db -Dliquibase.username=postgres -Dliquibase.password=postgres -Dliquibase.rollbackCount=1

# Scaling and infrastructure commands
# -----------------------------------------------------

# Scale application by adding new instances without stopping existing ones
# Usage: Update docker-compose.yml with new producer/worker entries, then run 'make scale' (edit numbers)
scale:
	docker-compose up -d producer_4 worker_6

# Reload NGINX configuration after updating nginx.conf
# Usage: Edit nginx.conf to add new producer/worker clusters, then run 'make reload-nginx'
reload-nginx:
	docker exec nginx-lb nginx -s reload

# Clear everyting :-(
clear:
	@if [ -n "$$(docker ps -aq)" ]; then docker rm -f $$(docker ps -aq); fi
	@if [ -n "$$(docker images -aq)" ]; then docker rmi -f $$(docker images -aq); fi
	@if [ -n "$$(docker volume ls -q)" ]; then docker volume rm $$(docker volume ls -q); fi
	@if [ -n "$$(docker network ls --filter "type=custom" -q)" ]; then docker network rm $$(docker network ls --filter "type=custom" -q); fi
	@docker system prune -a --volumes -f
