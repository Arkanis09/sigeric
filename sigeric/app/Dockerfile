# Etapa 1: compilar con Maven (no hace falta tener Java ni Maven instalados en la PC)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Etapa 2: imagen liviana solo con el JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=America/El_Salvador
COPY --from=build /src/target/sigeric.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-jar", "/app/app.jar"]
