(ns routes.news
  (:require [reitit.core :as r]
            [ring.util.http-response :as rr]
            [reitit.ring :as ring]
            [taoensso.timbre :as log]
            [clj-http.client :as http]
            [cheshire.core :as json]
            ))

(defn all-news
  [{{:keys [id]} :path-params node :xtdb-node :as req}]
  (-> (rr/ok {})
      (rr/content-type "application/json;charset=utf-8")))

(def news-routes
  (r/routes (r/router [["/api/v1/news"
                        ["" {:get all-news}]]])))