(ns routes.news
  (:require [reitit.core :as r]
            [ring.util.http-response :as rr]
            [reitit.ring :as ring]
            [taoensso.timbre :as log]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [xtdb.api :as xt]
            [common.util :as util]
            [common.xtdb.service :as xs]
            [services.news :as sn]
            ))

(defn all-news
  [{{:keys [id]} :path-params node :xtdb-node :as req}]
  (log/debugf "Returns the latest transaction to have been indexed by this node: %s" (xt/latest-completed-tx node))
  (log/debugf "Returns the latest transaction to have been submitted to this cluster: %s" (xt/latest-submitted-tx node))
  (log/debugf "Returns frequencies of indexed attributes: %s" (xt/attribute-stats node))
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
