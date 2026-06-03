FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY ./target/worker-*.jar worker.jar
ENTRYPOINT ["java", "-jar", "worker.jar"]
