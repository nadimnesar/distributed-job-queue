FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY ./target/worker-*.jar worker.jar

ENTRYPOINT ["java", "-jar", "worker.jar"]
