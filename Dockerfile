FROM eclipse-temurin:21-alpine
LABEL org.opencontainers.image.authors="m.buechner@dnb.de"
ENV TZ=Europe/Berlin
ENV MAVEN_CONFIG=/home/timeparsers/.m2
ENV MAVEN_OPTS="-Duser.home=/home/timeparsers"
RUN apk --no-cache add git maven && mkdir -p "$MAVEN_CONFIG"
WORKDIR /tmp
RUN git clone https://github.com/europeana/rd-normalisation-dates && apk del --no-network git
WORKDIR /tmp/rd-normalisation-dates
RUN mvn -B clean install -DskipTests && rm -rf /tmp/rd-normalisation-dates
COPY frontend /home/timeparsers
WORKDIR /home/timeparsers/
RUN mvn -B clean package
CMD ["java", "-Xms128M", "-Xmx512M", "-jar", "target/timeparsers.jar"]
EXPOSE 8080
