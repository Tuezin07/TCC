# Imagem com JDK para compilar o Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o pom.xml e baixa dependências (cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o restante do projeto e compila
COPY . .
RUN mvn clean package -DskipTests

# Etapa final — imagem leve só com o JDK de runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o .jar compilado
COPY --from=build /app/target/*.jar app.jar

# Expondo a porta padrão do Spring Boot
EXPOSE 8080

# Comando para rodar seu backend
CMD ["java", "-jar", "app.jar"]
