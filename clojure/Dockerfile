# Build image
FROM clojure:openjdk-11-tools-deps as build

ARG BUILD_DIR
ARG XTDB_DIR

WORKDIR $BUILD_DIR
VOLUME $XTDB_DIR
EXPOSE 3000

COPY deps.edn $BUILD_DIR/deps.edn
RUN clojure -Sforce -Spath >/dev/null

COPY src/ $BUILD_DIR/src
COPY resources/ $BUILD_DIR/resources
RUN clj -X:uberjar

FROM wang/alpine-openjdk11

ARG FILE
ARG APP

# Needed for RocksDB, Kafka
RUN apk update && apk add --no-cache libstdc++ && apk add --no-cache gcompat

ENV APPDIR /app
WORKDIR $APPDIR

COPY --from=build $FILE $APPDIR/$APP

CMD ["java", "-cp", "/app/core-server.jar", "core.handler"]