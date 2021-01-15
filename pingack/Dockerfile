FROM maven:3-jdk-8 as builder
WORKDIR /project
COPY . .
RUN mvn package spring-boot:repackage
FROM java:8-alpine
COPY --from=builder /project/pingack/target/pingack-0.0.1-SNAPSHOT.jar ./pingack.jar
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java -jar pingack.jar"]