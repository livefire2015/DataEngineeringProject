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
  (let [config {}]
    ;; (xt/start-node config)
    (xt/new-api-client "http://xtdb:3000")
    ))

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
