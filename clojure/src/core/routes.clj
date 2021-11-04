(ns core.routes
  (:require [reitit.core :as r]
            [ring.util.http-response :as rr]
            [reitit.ring :as ring]
            [taoensso.timbre :as log]
            [routes.news :as news]
            ))

(defn healthz-ok
  [req]
  (rr/ok "healthz"))

(defn healthz-ready
  [req]
  (rr/ok "ready"))

(defn root
  [req]
  (rr/ok "OK"))

(def default-routes
  (r/routes (r/router [["/" {:get root}]])))

(def healthz-routes
  (r/routes (r/router ["/healthz"
                       ["/" healthz-ok]
                       ["/ready" healthz-ready]])))

(def all-routes
  [default-routes
   news/news-routes
   healthz-routes])
