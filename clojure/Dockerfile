# Build image
FROM clojure:openjdk-11-tools-deps as build

ARG BUILD_DIR
WORKDIR $BUILD_DIR

COPY deps.edn $BUILD_DIR/deps.edn
RUN clj -e :ok

COPY src/ $BUILD_DIR/src
RUN clj -X:uberjar

FROM wang/alpine-openjdk11

ARG FILE
ARG APP

# Needed for RocksDB
# RUN apk add --no-cache libstdc++

ENV APPDIR /app
WORKDIR $APPDIR

COPY --from=build $FILE $APPDIR/$APP