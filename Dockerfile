FROM public.ecr.aws/docker/library/eclipse-temurin:21-alpine
LABEL org.opencontainers.image.authors="m.buechner@dnb.de"
ENV TZ=Europe/Berlin
ENV MAVEN_CONFIG=/home/timeparsers/.m2
ENV MAVEN_OPTS="-Duser.home=/home/timeparsers"
ENV TREETAGGER_HOME=/opt/treetagger
ENV PATH="${TREETAGGER_HOME}/cmd:${TREETAGGER_HOME}/bin:${PATH}"

# install Treetagger 3.2.5 and Libraries
RUN set -eux && \
    apk update && \
    apk add --no-cache perl && \
    apk add --no-cache --virtual .build-deps curl tar git gzip maven && \
    # Maven
    mkdir -p "${MAVEN_CONFIG}" && \
    # TreeTagger
    mkdir "${TREETAGGER_HOME}" && \
    curl -L -o "${TREETAGGER_HOME}/tree-tagger-linux-3.2.5.tar.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tree-tagger-linux-3.2.5.tar.gz" && \
    curl -L -o "${TREETAGGER_HOME}/tagger-scripts.tar.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tagger-scripts.tar.gz" && \
    curl -L -o "${TREETAGGER_HOME}/install-tagger.sh" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/install-tagger.sh" && \
    curl -L -o "${TREETAGGER_HOME}/german.par.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/german.par.gz" && \
    curl -L -o "${TREETAGGER_HOME}/german-chunker.par.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/german-chunker.par.gz" && \
    chmod +x "${TREETAGGER_HOME}/install-tagger.sh" && \
    cd "${TREETAGGER_HOME}" && \
    ( yes "" | "./install-tagger.sh" || true ) && \
    rm -f "${TREETAGGER_HOME}/*.tar.gz" "${TREETAGGER_HOME}/*.par.gz" "${TREETAGGER_HOME}/install-tagger.sh" && \
    # setze Leserechte für alle
    chmod -R a+rX "${TREETAGGER_HOME}"

# install Europeanas timeparser
RUN git clone https://github.com/europeana/rd-normalisation-dates "/tmp/rd-normalisation-dates" && \
    mvn -B -f "/tmp/rd-normalisation-dates/pom.xml" clean install -DskipTests && \
    rm -rf "/tmp/rd-normalisation-dates"

# install Timeparser
WORKDIR /home/timeparsers/
COPY frontend .
RUN mvn -B clean package

# cleanup
RUN apk del .build-deps;

CMD ["java", "-Xms128M", "-Xmx512M", "-jar", "target/timeparsers.jar"]
EXPOSE 8080
