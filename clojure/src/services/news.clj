(ns services.news
  (:require [taoensso.timbre :as log]
            [common.util :as util]
            [xtdb.api :as xt]
            [common.xtdb.service :as xs]))


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

(comment
  (def node (xt/start-node {:xtdb.lucene/lucene-store {}}))

  (->> (clojure.java.io/resource "sample_rss_news.edn")
       (util/load-edn)
       (xt/submit-tx node))

  (xt/q (xt/db node)
        {:find '[?e ?v ?s]
         :where '[[(text-search :language "en") [[?e ?v ?s]]]
                  [?e :xt/id]]})

  (xt/q (xt/db node)
        {:find '[?e ?v ?s]
         :where '[[(text-search :description "de*") [[?e ?v ?s]]]
                  [?e :xt/id]]})

  (xt/q (xt/db node)
        {:find '[?e ?v ?a ?s]
         :where '[[(wildcard-text-search "ron*") [[?e ?v ?a ?s]]]
                  [?e :xt/id]]}))

