FROM maven:3.9-eclipse-temurin-25-alpine AS build
ARG MODULE
WORKDIR /app
COPY . .
RUN mvn package -DskipTests -pl ${MODULE} -am -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
ARG MODULE
COPY --from=build /app/${MODULE}/target/${MODULE}-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
