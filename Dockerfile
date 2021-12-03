FROM openjdk:11.0.4-jre-slim
COPY ./build/libs/parser-*.jar application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]