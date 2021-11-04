(ns services.news
  (:require [taoensso.timbre :as log]
            [common.util :as util]
            [xtdb.api :as xt]
            [common.xtdb.service :as xs]
            ))

(def ^:const entity-type-news "rss_news")


(defn find-news-by-ids
  [node ids]
  (->> (xs/find-entities-by-ids node ids)
       (filter #(= (:entity/type %) entity-type-news))
       (map util/decrux)))

(defn find-all-news
  [node {:keys [limit]}]
  (->> {:limit limit}
       (xs/find-entities-by-attrs node {:entity/type entity-type-news})
       (map util/decrux)))
