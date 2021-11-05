(ns common.util
  (:require [config.core :refer [env]]
            [taoensso.timbre :as log]
            [java-time :as time]
            [clj-http.client :as http]
            [clojure.string :as string]
            [ring.util.codec :as codec]
            [clojure.walk :as walk]
            [camel-snake-kebab.core :as csk]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def ^:private us-ascii-charset (java.nio.charset.Charset/forName "US-ASCII"))
(def ^:private utf-8-charset (java.nio.charset.Charset/forName "UTF-8"))

(defn exception->str
  "Return a string with stacktrace in the exception"
  [e]
  (let [sw (java.io.StringWriter.)]
    (.printStackTrace e (java.io.PrintWriter. sw))
    (str sw)))

(defn uuid
  []
  (java.util.UUID/randomUUID))

(defn uuid-str
  []
  (-> (uuid)
      (str)))

(defn uuid-no-hyphen
  ([s]
   (-> (or s (uuid-str))
       (clojure.string/replace #"[^a-zA-Z0-9]" "")))
  ([]
   (uuid-no-hyphen nil)))

(defn str->uuid
  [s]
  (if s
    (java.util.UUID/fromString s)))

(defn str->uuid-safe
  [s]
  (try (java.util.UUID/fromString s)
    (catch java.lang.IllegalArgumentException e nil)))

(defn str->int
  "Return the int value of string"
  [str]
  (try
    (Integer/valueOf str)
    (catch Exception e
      (log/error e (format "Error parsing int string: %s." str)))))

(defn str->long
  "Return the long value of a string"
  [str]
  (if (not (string/blank? str))
    (try
      (Long/parseLong str)
      (catch Exception e
        (log/error e (format "Error parsing long string: %s." str))))))

(defn str->boolean
  "Return the boolean value of the string.
   Returns nil if the string is nil."
  [str]
  (if (not (string/blank? str))
    (boolean (java.lang.Boolean. str))))

(defn str->us-ascii
  [str]
  (. str getBytes us-ascii-charset))

(defn us-ascii->str
  [bytes]
  (String. bytes us-ascii-charset))

(defn str->utf-8
  [str]
  (. str getBytes utf-8-charset))

(defn utf-8->str
  [bytes]
  (String. bytes utf-8-charset))

(defn now []
  (time/offset-date-time))

(defn expired?
  [date]
  (time/before? date (now)))

(defn str->local-date
  [s]
  (if s
    (time/local-date "yyyy-MM-dd" s)))

(defn str->utc-date-time
  [s]
  (if s
    (time/zoned-date-time "yyyy-MM-dd HH:mm:ss z" s)))

(defn zoned-date-time->timestamp
  [t]
  (-> t
      (.toInstant)
      (java.sql.Timestamp/from)))

(defn local-date->str
  [d]
  (if d
    (time/format "yyyy-MM-dd" d)))

(defn date-str-yyyymmdd
  []
  (time/format "yyyyMMdd" (now)))

(defn date-str-yyyymmdd-hhmmss
  []
  (time/format "yyyy-MM-dd HH:mm:ss" (now)))

(defn days-after [t d]
  (time/plus t (time/days d)))

(defn days-from-now->date
  [d]
  (days-after (now) d))

(defn seconds-after [t s]
  (time/plus t (time/seconds s)))

(defn seconds-from-now->date
  [s]
  (seconds-after (now) s))

(defn long->timestamp [l]
  (time/offset-date-time (time/instant l) "UTC"))

(defn timestamp->long [t]
  (time/to-millis-from-epoch t))

(defn ->java-date [t]
  (time/java-date t))

(defn assoc-if
  "Same as assoc, but skip the assoc if v is nil"
  [m & kvs]
  (->> kvs
       (partition 2)
       (filter second)
       (map vec)
       (into m)))

(defn remove-nil-values
  [m]
  (apply dissoc m
         (for [[k v] m :when (nil? v)] k)))

(defn update-in-if-exists
  "'Updates' a value in a nested associative structure, where ks is a
   sequence of keys and f is a function that will take the old value
   and any supplied args and return the new value, and returns a new
   nested structure.  If the key at any level does not exist, hash-maps
   will NOT be created or updated."
  ([m [k & ks] f & args]
   (if (get m k)
     (if ks
       (assoc m k (apply update-in-if-exists (get m k) ks f args))
       (assoc m k (apply f (get m k) args)))
     m)))

(defn parse-comma-str [str]
  (if (not (string/blank? str))
    (->> (string/split str #",")
         (mapv string/trim))))

(defn parse-str-ids [str-ids]
  (if (not (string/blank? str-ids))
    (let [ids (map str->uuid (parse-comma-str str-ids))
          filtered (filter (comp not nil?) ids)]
      (if (= (count ids) (count filtered))
        ids
        nil))))

(defn map->nsmap
  [m n]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str n) (name k))
                              k)]
                 (assoc acc new-kw v)))
             {} m))

(defn query-params-str
  [params]
  (let [params (remove-nil-values params)]
    (when-not (empty? params)
      (str "?" (codec/form-encode params)))))

(defn coerce-to-int [n]
  (cond-> n
    (string? n)  (Integer/parseInt)))

(defn decrux
  [m]
  (-> m
      (dissoc :xtdb.api
              :xtdb.api/tx-time
              :xtdb.api/tx-ops
              :crux.tx
              :crux.tx/tx-time
              :crux.tx/tx-ops
              :entity/type)
      (clojure.set/rename-keys {:xt/id :id})))

(defn populate-last-modified-time
  [m]
  (assoc m :last-modified-at (->java-date (now))))

(defn populate-created-at-time
  [m]
  (assoc m :created-at (->java-date (now))))

(defn populate-deleted-at-time
  [m]
  (merge {:deleted-at (->java-date (now))}
         m))

(defn populate-last-heartbeat-time
  [m]
  (assoc m :last-heartbeat-at (->java-date (now))))

(defn no-heartbeat?
  ([t s]
   (-> (time/offset-date-time t "UTC")
       (seconds-after s)
       (expired?)))
  ([t]
   (no-heartbeat? t 30)))

(defn decode [to-decode]
  (String. (.decode (java.util.Base64/getDecoder) to-decode)))

(defn transform-keys
  "Recursively transforms all map keys from strings or keywords to result of fn."
  {:added "1.1"}
  [m key-fn]
  (let [f (fn [[k v]] (if (or (string? k)
                              (keyword? k))
                        [(key-fn k) v]
                        [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn aggregate-records
  "Aggregate list of records, result map will use value of k1 in the record as key,
   aggregated list of value of k2 in the record will be the value in the result map.

   => (aggregate-records [{:bucket_id 34, :thing_id 23}
                          {:bucket_id 35, :thing_id 23}
                          {:bucket_id 35, :thing_id 24}] :thing_id :bucket_id)
   {24 (35), 23 (35 34)}"
  [records k1 k2]
  (reduce (fn [m record] (update-in m [(get record k1)] conj (get record k2))) {} records))

(defn order-by
  ([order coll key-fn]
   (sort-by #(.indexOf order (key-fn %)) coll))
  ([order coll]
   (order-by order coll identity)))

(defn camelize
  [x]
  (cond
    (map? x)
    (transform-keys x csk/->camelCaseKeyword)

    (coll? x)
    (map #(transform-keys % csk/->camelCaseKeyword) x)))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (log/errorf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (log/errorf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))
