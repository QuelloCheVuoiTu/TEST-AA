FROM eclipse-temurin:21-jdk
USER root
RUN apt-get update && apt-get install -y bash vim curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia il JAR dell'app principale (es. PisteKartItalia-1.0-SNAPSHOT.jar)
COPY target/*.jar app.jar

# L'app principale risponde sulla 8180
EXPOSE 8180

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]