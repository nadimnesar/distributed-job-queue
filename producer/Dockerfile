FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY ./target/producer-*.jar producer.jar

ENTRYPOINT ["java", "-jar", "producer.jar"]
