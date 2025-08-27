# syntax=docker/dockerfile:1

############################
# 1) Build stage (Maven)
############################
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only pom first to warm dependency cache
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

# Now copy sources and build
COPY src ./src
RUN mvn -B -DskipTests package

############################
# 2) Runtime stage (JRE)
############################
FROM eclipse-temurin:17-jre-jammy

# Hugging Face Spaces containers run with UID 1000; create matching user to avoid permission issues
# (Recommended in Docker Spaces docs)
RUN useradd -m -u 1000 appuser
USER appuser
WORKDIR /home/appuser

# Bring in built jar
COPY --from=build /workspace/target/*.jar app.jar

# Default port Spaces expose is 7860; the README's app_port can change it.
# We'll respect $PORT if Spaces set it; otherwise default to 7860.
ENV PORT=7860
ENV JAVA_OPTS=""

# Optional: Expose for local runs
EXPOSE 7860

# Run Spring on the Space port, bind to 0.0.0.0 so the Space can reach it
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /home/appuser/app.jar --server.port=${PORT} --server.address=0.0.0.0"]
