FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY ./target/producer-*.jar producer.jar

ENTRYPOINT ["sh", "-c", "export LOGGING_FILE_NAME=\"/logs/producer-$(hostname).log\" && exec java -jar producer.jar"]
