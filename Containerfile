FROM eclipse-temurin:25-jdk@sha256:8c0a84ea11c8f6ed52600fc19f1040121f2a162998e9f50a5faebbbad9172dcc AS build

RUN mkdir /build
COPY pom.xml /build/
COPY src /build/src/
COPY .mvn /build/.mvn
COPY mvnw /build/mvnw
WORKDIR /build
RUN ./mvnw clean package spring-boot:repackage

# Note: The default non root chainguard user is 65532
FROM cgr.dev/chainguard/jdk@sha256:325b07100ec5d02524e263fa56ab66ad1566a72fd62b641cf8cd5d6bb463ac29

USER 0
RUN mkdir -p /opt/.logs \
    && mkdir -p /apidef
RUN chown -R 65532:65532 /opt/
USER 65532
COPY --from=build /build/target/dcat-ap-se-processor-0.0.3-SNAPSHOT.jar /opt/app.jar

ENV JDK_JAVA_OPTIONS -Duser.language=sv-SE -Duser.region=SE -Duser.timezone=Europe/Stockholm
WORKDIR /opt
USER 0
RUN chmod +x app.jar
USER 65532
ENV PORT 8080
EXPOSE 8080
CMD ["java","-jar","app.jar"]