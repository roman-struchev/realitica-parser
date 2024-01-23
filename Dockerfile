FROM openjdk:21-slim
COPY ./build/libs/parser-*.jar application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]