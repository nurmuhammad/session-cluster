# App image — oldindan qurilgan jar'ni JRE ustiga qo'yadi.
# Avval hostda quring:  ./gradlew :app:build   (yoki app/ ichida: gradle build)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/session-app-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
