# Use Eclipse Temurin as base image (maintained OpenJDK distribution)
FROM eclipse-temurin:11-jre-slim

# Set working directory
WORKDIR /app

# Copy lib files
COPY lib/ /app/lib/

# Copy source files
COPY backend/src/ /app/backend/src/

# Compile Java files
RUN javac -cp "lib/*" -d out backend/src/*.java

# Expose port
EXPOSE 8080

# Set environment variables
ENV PORT=8080

# Run the application
CMD ["java", "-cp", "out:lib/*", "backend.src.Main"]
