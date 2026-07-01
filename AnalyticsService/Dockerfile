FROM therapy-jdk25
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 5006
ENTRYPOINT ["java", "-jar", "app.jar"]
