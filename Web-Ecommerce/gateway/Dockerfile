FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common/pom.xml common/
COPY services/user-service/pom.xml services/user-service/
COPY services/product-service/pom.xml services/product-service/
COPY services/order-service/pom.xml services/order-service/
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl gateway -am -B
COPY common/src common/src/
COPY gateway/src gateway/src/
RUN mvn package -pl gateway -am -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
