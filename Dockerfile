# Etapa 1: build
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests


# Etapa 2: runtime
FROM eclipse-temurin:21-jdk

WORKDIR /app

# libs necessárias pro AWT/Swing + X11
RUN apt-get update && apt-get install -y \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfreetype6 \
    libfontconfig1 \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/CipherMQ-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-cp", "app.jar", "com.manocorbas.ciphermq.App"]