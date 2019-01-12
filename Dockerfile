FROM maven:3.6.0-jdk-8 AS builder
COPY pom.xml /build/pom.xml
WORKDIR /build/
# cache dependencies
RUN ["mvn", "-B", "-T", "4", "dependency:resolve", "dependency:resolve-plugins", "dependency:go-offline", "verify", "package", "--fail-never"]
COPY src/ /build/src/
RUN ["mvn", "-B", "-T", "4", "package"]

FROM openjdk:8-jre
COPY --from=builder /build/target/eu.tanov.gps.gpxmergeheartrate-1.0-SNAPSHOT.jar /app/eu.tanov.gps.gpxmergeheartrate-1.0-SNAPSHOT.jar
RUN ["mkdir", "/data"]
WORKDIR /data
ENTRYPOINT ["java", "-jar", "/app/eu.tanov.gps.gpxmergeheartrate-1.0-SNAPSHOT.jar"]
