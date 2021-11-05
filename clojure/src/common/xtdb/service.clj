(ns common.xtdb.service
  (:require [xtdb.api :as xt]
            [config.core :refer [env]]
            [clojure.java.io :as io]))

(def ^:private gen-sym (comp gensym name))

(defn- gen-uuid*
  []
  (java.util.UUID/randomUUID))

(defn create-entity
  [node params]
  (let [xtdb-id (:xt/id params (gen-uuid*))
        xtdb-tx (xt/submit-tx
                 node
                 [[:xtdb.api/put
                   (assoc params
                          :xt/id xtdb-id
                          )
                   (:xtdb.api/valid-time params)]])]
    (-> params
        (assoc :xt/id xtdb-id)
        )))

(defn create-entity-sync
  [node params]
  (let [xtdb-id (:xt/id params (gen-uuid*))
        xtdb-tx (xt/submit-tx
                 node
                 [[:xtdb.api/put
                   (assoc params
                          :xt/id xtdb-id
                          )
                   (:xtdb.api/valid-time params)]])]
    (xt/await-tx node xtdb-tx)
    (-> params
        (assoc :xt/id xtdb-id)
        )))

(defn retrieve-entity-by-id
  [node id]
  (xt/entity (xt/db node) id))

(defn find-entity-by-id
  [node id]
  (xt/pull (xt/db node)
           ['*]
           id))

(defn find-entities-by-ids
  [node ids]
  {:pre [(vector? ids)]}
  (xt/pull-many (xt/db node)
                ['*]
                ids))

(defn find-entities-by-attrs
  [node attrs {:keys [limit]}]
  {:pre [(map? attrs)
         (int? limit)]}
  (let [vs (into [] (for [[_ v] attrs] v))
        syma (into {} (for [[k _] attrs] [k (gen-sym k)]))]
    (->> (xt/q (xt/db node)
               `{:find [~'(pull ?e [*])]
                 :where ~(reduce
                          (fn [q [a v]]
                            (conj q ['?e a (get syma a)]))
                          []
                          attrs)
                 :in ~[(reduce
                        (fn [q [a _]]
                          (conj q (get syma a)))
                        []
                        attrs)]
                 :limit ~limit
                 }
               vs)
         (map first) )))

