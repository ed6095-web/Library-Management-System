# Use Amazon Corretto OpenJDK
FROM amazoncorretto:11

# Set working directory
WORKDIR /app

# Copy lib files
COPY lib/ /app/lib/

# Download PostgreSQL JDBC driver
RUN curl -o /app/lib/postgresql-42.7.1.jar https://jdbc.postgresql.org/download/postgresql-42.7.1.jar

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
