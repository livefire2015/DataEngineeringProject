(ns common.middleware.node
  (:require [xtdb.api :as xt]
            [config.core :as config :refer [env]]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [mount.core :as mount]
            )
  (:import [xtdb.api IXtdb]))

(defn- start-xtdb-node
  []
  (let [config {:kafka-config {:xtdb/module 'xtdb.kafka/->kafka-config
                               :bootstrap-servers "localhost:9092"
                               }
                :xtdb/tx-log {:xtdb/module 'xtdb.kafka/->tx-log
                              :kafka-config :kafka-config
                              }
                :xtdb/document-store {:xtdb/module 'xtdb.kafka/->document-store
                                      :kafka-config :kafka-config
                                      }
                :xtdb.http-server/server {:port 3000}
                }]
    (xt/start-node config)))

(mount/defstate xtdb-node
  :start (do
           (log/info "Starting xtdb-node...")
           (let [node (start-xtdb-node)]
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
