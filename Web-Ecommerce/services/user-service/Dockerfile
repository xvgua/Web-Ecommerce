FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY settings-docker.xml /root/.m2/settings.xml
COPY pom.xml .
COPY common/pom.xml common/
COPY services/user-service/pom.xml services/user-service/
COPY services/product-service/pom.xml services/product-service/
COPY services/order-service/pom.xml services/order-service/
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl services/user-service -am -B
COPY common/src common/src/
COPY services/user-service/src services/user-service/src/
RUN mvn package -pl services/user-service -am -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/services/user-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
