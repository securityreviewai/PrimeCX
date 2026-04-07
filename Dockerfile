FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY backend/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

COPY backend/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S primecx && adduser -S primecx -G primecx

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R primecx:primecx /app

USER primecx

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
