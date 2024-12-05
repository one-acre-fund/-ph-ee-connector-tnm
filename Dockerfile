FROM eclipse-temurin:17-alpine AS build

WORKDIR /ph-ee-connector-tnm

COPY . .

RUN if ls build/libs/ph-ee-connector-tnm-*.jar 1>  /dev/null 2>&1 ; then echo "Using Already built JAR";  \
    else ./gradlew bootJar ; fi

FROM eclipse-temurin:17-alpine

WORKDIR /app

COPY --from=build /ph-ee-connector-tnm/build/libs/ph-ee-connector-tnm*.jar ph-ee-connector-tnm.jar

EXPOSE 5000
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/ph-ee-connector-tnm.jar"]