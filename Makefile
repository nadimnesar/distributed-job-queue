start: mvn-build docker-up

stop: docker-down

reload: docker-down docker-up

mvn-build:
	mvn clean install -DskipTests

docker-up:
	 docker-compose up -d

docker-down:
	docker-compose down --remove-orphans

# To add new instances while not stopping existing ones
# Add new producer/worker in docker-compose.yml file and run scale (edit numbers)
scale:
	docker-compose up -d producer_4 worker_6

# Add new producer/worker cluster in nginx.conf file, then reload nginx
reload-nginx:
	docker exec nginx-lb nginx -s reload

clear:
	docker rm -f $(shell docker ps -aq) || true
	docker rmi -f $(shell docker images -aq) || true
	docker volume rm $(shell docker volume ls -q) || true
	docker network rm $(shell docker network ls -q) || true
	docker system prune -a --volumes -f || true
