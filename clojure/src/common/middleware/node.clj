(ns common.middleware.node
  (:require [xtdb.api :as xt]
            [config.core :as config :refer [env]]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [mount.core :as mount])
            
  (:import [xtdb.api IXtdb]))

(defn- start-xtdb-node
  []
  (let [config {:xtdb/index-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store, :db-dir "/var/lib/xtdb/indexes"}}
                :xtdb/document-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store, :db-dir "/var/lib/xtdb/documents"}}
                :xtdb/tx-log {:xtdb/module 'xtdb.kafka/->tx-log
                              :kafka-config {:bootstrap-servers "kafka:9092"}
                              }
                :xtdb.http-server/server {:port 3000
                                          :jetty-opts {:host "0.0.0.0"}}}]
    (xt/start-node config))
    ;; (xt/new-api-client "http://xtdb:3000")
  )

(mount/defstate xtdb-node
  :start (do
           (log/info "Starting xtdb-node...")
           (let [^IXtdb node (start-xtdb-node)]
             (log/info "Started Syncing node...")
             (xt/sync node)
             (log/info "STARTED xtdb-node.")
             node))
  :stop (do
          (log/info "Stop xtdb-node")
          (.close xtdb-node)))

(defn wrap-xtdb-node
  [handler]
  (fn [req]
    (->> xtdb-node
         (assoc req :xtdb-node)
         handler)))
