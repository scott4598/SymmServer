# Use a lightweight OpenJDK runtime (2026 standard is Temurin - set to Java8)
FROM eclipse-temurin:8-jre-focal

# Create a directory for the app
WORKDIR /app

# Should create a work directory - give the container a place to store files it downloads (unsure if above is still needed)
RUN mkdir -p /app/temp_analysis && chmod 777 /app/temp_analysis

# Copy your PRE-BUILT jar into the container
COPY 1.0-0.0.1-SNAPSHOT-jar-with-dependencies.jar analyzer-engine.jar

# Security: Run as a non-root user (Ubuntu/Focal syntax)
RUN groupadd -r research && useradd -r -g research analyzer
USER analyzer

# Tell the app where Nexus is (can be overridden in docker-compose)
ENV NEXUS_URL=http://nexus:8081

# Tell Docker the container listens on 5000
EXPOSE 5000

# The command to run the jar
ENTRYPOINT ["java", "-jar", "analyzer-engine.jar"]
