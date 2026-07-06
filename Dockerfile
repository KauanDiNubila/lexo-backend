FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY cliente-service/pom.xml cliente-service/
COPY processo-service/pom.xml processo-service/
COPY financeiro-service/pom.xml financeiro-service/
COPY auditoria-service/pom.xml auditoria-service/
COPY notificacao-service/pom.xml notificacao-service/
COPY ia-service/pom.xml ia-service/
RUN mvn -B -ntp -q dependency:go-offline || true
COPY . .
RUN mvn -B -ntp -q -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
ARG SERVICE
WORKDIR /app
COPY --from=build /app/${SERVICE}/target/${SERVICE}-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
