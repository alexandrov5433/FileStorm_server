FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests \
  && ls -lh target

FROM eclipse-temurin:24-jdk AS runtime
WORKDIR /app

COPY --from=build /app/target/*SNAPSHOT.jar FileStorm_server.jar

EXPOSE 443

ENTRYPOINT ["java", "-jar", "FileStorm_server.jar"]