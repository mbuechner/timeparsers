FROM eclipse-temurin:21-alpine
LABEL org.opencontainers.image.authors="m.buechner@dnb.de"
ENV TZ=Europe/Berlin
RUN apk --no-cache add git maven
WORKDIR /tmp
RUN git clone https://github.com/europeana/rd-normalisation-dates && apk del --no-network git
WORKDIR /tmp/rd-normalisation-dates
RUN mvn clean install -DskipTests && rm -rf /tmp/rd-normalisation-dates
COPY frontend /home/timeparsers
WORKDIR /home/timeparsers/
RUN mvn clean package
ENV RUN_USER=nobody
ENV RUN_GROUP=0
CMD ["mvn", "spring-boot:run"]
EXPOSE 8080
