FROM openjdk:17-jdk
WORKDIR /app
ADD ./target/betting-service-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
CMD ["java", "-jar", "betting-service-0.0.1-SNAPSHOT.jar"]
