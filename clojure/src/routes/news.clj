(ns routes.news
  (:require [reitit.core :as r]
            [ring.util.http-response :as rr]
            [reitit.ring :as ring]
            [taoensso.timbre :as log]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [common.util :as util]
            [services.news :as sn]
            ))

(defn all-news
  [{{:keys [id]} :path-params node :xtdb-node :as req}]
  (-> (cond
        (some? id)
        (->> (sn/find-news-by-ids node [id])
             )
        
        :default
        (sn/find-all-news node {:limit 100}))
      (util/camelize)
      (rr/ok)
      (rr/content-type "application/json;charset=utf-8")))

(def news-routes
  (r/routes (r/router [["/api/v1/news"
                        ["" {:get all-news
                             }]
                        ["/:id" {:get all-news
                                 }]]])))
