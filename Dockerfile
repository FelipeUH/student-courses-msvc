# syntax=docker/dockerfile:1
# Dockerfile único para todos los servicios. El módulo se elige con el build arg MODULE:
#   docker build --build-arg MODULE=microservice-student -t msvc-student .
# El contexto de build es la raíz del repositorio, porque Maven necesita el POM padre.

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace

# POM padre y los de todos los módulos (el reactor los necesita aunque solo se compile uno)
COPY pom.xml .
COPY microservice-config/pom.xml microservice-config/
COPY microservice-eureka/pom.xml microservice-eureka/
COPY microservice-student/pom.xml microservice-student/
COPY microservice-course/pom.xml microservice-course/
COPY microservice-gateway/pom.xml microservice-gateway/

COPY ${MODULE}/src ${MODULE}/src

# Los tests se omiten: necesitan el Config Server en marcha.
# La caché de ~/.m2 se comparte entre los builds de todos los servicios.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests \
    && cp ${MODULE}/target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home spring
COPY --from=build /workspace/app.jar app.jar
USER spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
