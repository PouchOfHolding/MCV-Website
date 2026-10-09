FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S bootuser -G spring

WORKDIR /app

COPY --chown=bootuser:spring target/mcv-0.0.1.jar mcv-0.0.1.jar

ENTRYPOINT ["java","-jar","mcv-0.0.1.jar"]