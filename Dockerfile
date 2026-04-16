FROM public.ecr.aws/docker/library/eclipse-temurin:25-alpine AS builder
LABEL org.opencontainers.image.authors="m.buechner@dnb.de"

ENV TZ=Europe/Berlin
ENV MAVEN_CONFIG=/root/.m2
ENV MAVEN_OPTS="-Duser.home=/root"
ENV TREETAGGER_HOME=/opt/treetagger
ENV PATH="${TREETAGGER_HOME}/cmd:${TREETAGGER_HOME}/bin:${PATH}"

RUN set -eux && \
    apk add --no-cache perl curl tar git gzip maven && \
    mkdir -p "${MAVEN_CONFIG}" "${TREETAGGER_HOME}"

RUN set -eux && \
    curl -L -o "${TREETAGGER_HOME}/tree-tagger-linux-3.2.5.tar.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tree-tagger-linux-3.2.5.tar.gz" && \
    curl -L -o "${TREETAGGER_HOME}/tagger-scripts.tar.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tagger-scripts.tar.gz" && \
    curl -L -o "${TREETAGGER_HOME}/install-tagger.sh" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/install-tagger.sh" && \
    curl -L -o "${TREETAGGER_HOME}/german.par.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/german.par.gz" && \
    curl -L -o "${TREETAGGER_HOME}/german-chunker.par.gz" "https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/german-chunker.par.gz" && \
    chmod +x "${TREETAGGER_HOME}/install-tagger.sh" && \
    cd "${TREETAGGER_HOME}" && \
    ( yes "" | ./install-tagger.sh || true ) && \
    rm -f ${TREETAGGER_HOME}/*.tar.gz ${TREETAGGER_HOME}/*.par.gz "${TREETAGGER_HOME}/install-tagger.sh" && \
    chmod -R a+rX "${TREETAGGER_HOME}"

WORKDIR /build
COPY .mvn/ ./.mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x ./mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B clean package -DskipTests

FROM public.ecr.aws/docker/library/eclipse-temurin:25-alpine AS runtime
ENV TZ=Europe/Berlin
ENV TREETAGGER_HOME=/opt/treetagger
ENV PATH="${TREETAGGER_HOME}/cmd:${TREETAGGER_HOME}/bin:${PATH}"

RUN set -eux && \
    addgroup -S app && \
    adduser -S -G app app && \
    apk add --no-cache perl

WORKDIR /app
COPY --from=builder /opt/treetagger /opt/treetagger
COPY --from=builder /build/target/timeparsers.jar ./timeparsers.jar
RUN chown -R app:app /app /opt/treetagger

USER app
EXPOSE 8080
CMD ["java", "-Xms512M", "-Xmx1G", "-jar", "/app/timeparsers.jar"]
