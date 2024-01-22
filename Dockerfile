FROM openjdk:17-alpine
COPY ./build/libs/parser-*.jar application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]