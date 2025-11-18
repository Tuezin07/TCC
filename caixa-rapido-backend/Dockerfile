# Etapa de build usando Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o pom.xml e baixa dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o restante do projeto e compila
COPY . .
RUN mvn clean package -DskipTests

# Etapa final — runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o .jar compilado da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Porta usada pelo Spring Boot
EXPOSE 8080

# Comando que inicia o backend no Render
CMD ["java", "-jar", "app.jar"]
