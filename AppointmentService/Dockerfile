FROM therapy-jdk25
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 5005
ENTRYPOINT ["java", "-jar", "app.jar"]
