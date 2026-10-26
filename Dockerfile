FROM eclipse-temurin:21-jdk-jammy AS build
# The wrapper's pinned checksum is for the ZIP distribution.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -DskipTests -Daether.connector.connectTimeout=15000 -Daether.connector.requestTimeout=30000 package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/grade-scale.jar app.jar
USER app
ENV SERVER_ADDRESS=0.0.0.0 PORT=8081
EXPOSE 8081
ENTRYPOINT ["java", "-Xms32m", "-Xmx192m", "-jar", "/app/app.jar"]
