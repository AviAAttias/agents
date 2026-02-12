FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -pl reporting-worker -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/reporting-worker/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
