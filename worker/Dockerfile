FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY ./target/worker-*.jar worker.jar

ENTRYPOINT ["sh", "-c", "export LOGGING_FILE_NAME=\"/logs/worker-$(hostname).log\" && exec java -jar worker.jar"]
