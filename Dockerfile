FROM maven:3-eclipse-temurin-21-alpine AS mchain
RUN apk --update --no-cache add git
COPY frontend /tmp/frontend
RUN git clone https://github.com/europeana/rd-normalisation-dates /tmp/external/rd-normalisation-dates
WORKDIR /tmp/external/rd-normalisation-dates
RUN mvn install -DskipTests
WORKDIR /tmp/frontend
RUN mvn package

FROM eclipse-temurin:21-alpine
LABEL org.opencontainers.image.authors="m.buechner@dnb.de"
ENV TZ=Europe/Berlin
RUN mkdir /home/timeparsers
WORKDIR /home/timeparsers/
COPY --from=mchain /tmp/frontend/target/timeparsers.jar timeparsers.jar
CMD ["java", "-Xms128M", "-Xmx512M", "-jar", "timeparsers.jar"]
EXPOSE 8080
