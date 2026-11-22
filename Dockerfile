# syntax=docker/dockerfile:1
# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Copy the POM first so the dependency layer is cached between source changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/banker-platform.jar extract --layers --destination target/extracted

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# Non-root user (OpenShift additionally runs with an arbitrary UID in group 0, hence chgrp/chmod)
RUN useradd --system --uid 10001 --gid 0 banker \
 && mkdir -p /var/lib/kafka-streams && chgrp -R 0 /var/lib/kafka-streams && chmod -R g=u /var/lib/kafka-streams
# Spring Boot layers: least frequently changing first => smaller image pushes.
# Result: /app/banker-platform.jar + /app/lib/*.jar
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
# Startup optimization: Class Data Sharing. A training run starts the Spring context and exits before
# any Lifecycle bean (Kafka, Tomcat) starts, so no broker is needed here. Measured locally: ~3.5 s -> ~2.3 s.
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh \
         -Dspring.kafka.admin.auto-create=false -jar banker-platform.jar > /dev/null 2>&1 \
 && test -s app.jsa
USER 10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    KAFKA_STREAMS_STATE_DIR=/var/lib/kafka-streams
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-Xlog:cds=off", "-Xlog:cds+dynamic=off", "-jar", "banker-platform.jar"]
