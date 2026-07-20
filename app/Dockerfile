# App image — oldindan qurilgan jar'ni JRE ustiga qo'yadi.
# Avval hostda quring:  mvn -f app/pom.xml clean package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/session-app-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
