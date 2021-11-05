# Kafka Connect Question

Although I seem to be able to get my kafka sink connector working, I am
not able to query data from my XTDB node. Below please find my
setup and some logging. What might be missing/wrong?

1. xtdb deps

```edn
{com.xtdb/xtdb-core {:mvn/version "1.19.0"}
 com.xtdb/xtdb-kafka {:mvn/version "1.19.0"}
 com.xtdb/xtdb-http-server {:mvn/version "1.19.0"}
 com.xtdb/xtdb-rocksdb {:mvn/version "1.19.0"}}
```

2. node config

```clojure
(def ^xtdb.api.IXtdb node
  (xt/start-node {:xtdb/index-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store, :db-dir "/var/lib/xtdb/indexes"}}
                  :xtdb/document-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store, :db-dir "/var/lib/xtdb/documents"}}
                  :xtdb/tx-log {:xtdb/module 'xtdb.kafka/->tx-log
                                :kafka-config {:bootstrap-servers "kafka:9092"}
                                }
                  :xtdb.http-server/server {:port 3000
                                            :jetty-opts {:host "0.0.0.0"}}}))
```

3. connector setup

```bash
confluent-hub install --no-prompt juxt/kafka-connect-crux:19.12-1.6.1-alpha
```

```json
{
    "name":"xtdb-sink",
    "config": {
        "connector.class":"crux.kafka.connect.CruxSinkConnector",
        "tasks.max":"1",
        "topics":"rss_news",
        "key.converter":"org.apache.kafka.connect.json.JsonConverter",
        "key.converter.schemas.enable":false,
        "value.converter":"org.apache.kafka.connect.json.JsonConverter",
        "value.converter.schemas.enable":false,
        "url":"http://clojure:3000",
        "id.key":"xt/id"
    }
}
```

4. some logging

* connector docker container

Does this mean that the sink connector is working?

```
......
[2021-11-04 04:41:59,489] INFO sink record: #object[org.apache.kafka.connect.sink.SinkRecord 0x5443eb7 SinkRecord{kafkaOffset=467, timestampType=CreateTime} ConnectRecord{topic='rss_news', kafkaPartition=0, key=null, keySchema=null, value={author=dfb, entity/type=rss_news, link=https://www.dfb.de/news/detail/mit-24-spielern-nach-frankreich-und-portugal-233994/?no_cache=1&cHash=2a4d8ef0fd5cec27c65fa288393c6124, description=Die U 20 ist in dieser Saison noch ungeschlagen, nun wartet ein anspruchsvoller Jahresabschluss. Trainer Christian Wörns hat 24 Spieler für die Auswärtsspiele gegen Frankreich am 11. November und Portugal am 15. November nominiert., language=de, published=2021-11-02 09:00:00, title=Mit 24 Spielern nach Frankreich und Portugal, xt/id=1c2ef1e0-b3e5-453d-8a98-c4f33aa7498c}, valueSchema=null, timestamp=1636000919487, headers=ConnectHeaders(headers=)}] (crux.kafka.connect)

[2021-11-04 04:41:59,489] INFO tx op: [:crux.tx/put {:description Die U 20 ist in dieser Saison noch ungeschlagen, nun wartet ein anspruchsvoller Jahresabschluss. Trainer Christian Wörns hat 24 Spieler für die Auswärtsspiele gegen Frankreich am 11. November und Portugal am 15. November nominiert., :entity/type rss_news, :title Mit 24 Spielern nach Frankreich und Portugal, :author dfb, :language de, :link https://www.dfb.de/news/detail/mit-24-spielern-nach-frankreich-und-portugal-233994/?no_cache=1&cHash=2a4d8ef0fd5cec27c65fa288393c6124, :xt/id 1c2ef1e0-b3e5-453d-8a98-c4f33aa7498c, :crux.db/id #crux/id 1c2ef1e0-b3e5-453d-8a98-c4f33aa7498c, :published 2021-11-02 09:00:00}] (crux.kafka.connect)

[2021-11-04 04:42:13,782] INFO WorkerSinkTask{id=xtdb-sink-0} Committing offsets asynchronously using sequence number 3: {rss_news-0=OffsetAndMetadata{offset=468, leaderEpoch=null, metadata=''}} (org.apache.kafka.connect.runtime.WorkerSinkTask)
......
```

* clojure server container

Does this mean that none of the sink transactions have been indexed to the node?

```
......
2021-11-04T06:17:52.491Z b29f1c572f9b DEBUG [routes.news:16] - Returns the latest transaction to have been indexed by this node: nil

2021-11-04T06:17:52.494Z b29f1c572f9b DEBUG [routes.news:17] - Returns the latest transaction to have been submitted to this cluster: nil

2021-11-04T06:17:52.507Z b29f1c572f9b DEBUG [routes.news:18] - Returns frequencies of indexed attributes: {}
......
```

This returns empty as well.

```clojure
(xt/pull (xt/db node)
         ['*]
        "1c2ef1e0-b3e5-453d-8a98-c4f33aa7498c")
```



# Docker Compose Network 

Putting it shortly, docker compose isolates each of the containers network. This is done by adding them to the <myapp>_default which is a newly created network, where <myapp> is the name of the directory. Under this each of the containers is added via it’s name. In our case that would be postgres and server. These are like DNS A-records, which means using postgres would resolve to the IP of the postgres container and so on. Each of the container can use these names to connect to the other containers.
