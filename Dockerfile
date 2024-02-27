FROM openjdk:21-slim
COPY ./build/libs/realitica-parser-*.jar application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]