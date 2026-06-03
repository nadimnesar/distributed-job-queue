FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY ./target/producer-*.jar producer.jar
ENTRYPOINT ["java", "-jar", "producer.jar"]
