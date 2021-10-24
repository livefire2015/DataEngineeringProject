(ns core.handler
  (:require [ring.adapter.undertow :refer [run-undertow]]
            [mount.core :as mount]
            [core.routes :as routes]
            [reitit.ring :as ring]
            [ring.middleware.reload :refer [wrap-reload]]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.coercion :as rrc]
            [taoensso.timbre :as log]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.middleware.exceptions :as exceptions :refer [wrap-exceptions]]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.middleware :refer [wrap-format]]
            [muuntaja.core :as m]
            [muuntaja.format.json :as json-format]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [jwt-backend.jws :as jws-backend]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [config.core :as config])
  (:gen-class))

;; https://cljdoc.org/d/metosin/reitit/0.5.11/doc/ring/pluggable-coercion#pretty-printing-spec-errors
(defn create-error-handler [status]
  (let [handler (exception/create-coercion-handler status)]
    (fn [exception request id]
      (log/debug "problems:" (-> exception ex-data :problems))
      (handler exception request))))

(defn wrap-debug [handler]
  (fn [req]
    (log/debug "Method:" (:request-method req) "URI:" (:uri req))
    (let [resp (handler req)]
      (log/debug "RESP:" resp)
      resp)))

(def app (-> (ring/ring-handler (ring/router routes/all-routes
                                             {:data {:middleware [rrc/coerce-request-middleware
                                                                  muuntaja/format-request-middleware
                                                                  parameters/parameters-middleware
                                                                  ; wrap-enforce-roles
                                                                  ]}})
                                {:not-found (constantly {:status 404, :body "404: Not Found!"})
                                 :method-not-allowed (constantly {:status 405, :body "405: Method not allowed!"})
                                 :not-acceptable (constantly {:status 406, :body "406: Not Acceptable!"})})
             (wrap-authentication (jws-backend/jws-backend {:secret (:jws-secret config/env)
                                                            :token-name "Bearer"}))
             (wrap-cookies)
             (wrap-exceptions {:error-fns (merge exceptions/default-error-fns
                                                 {:reitit.coercion/request-coercion (create-error-handler 400)
                                                  :reitit.coercion/response-coercion (create-error-handler 500)
                                                  :selmer/validation-error (create-error-handler 500)})
                               :pre-hook (fn [ex req id]
                                           (log/debug ex))})
             (wrap-resource "public/dist")
             wrap-keyword-params
             wrap-params
             wrap-json-response
             wrap-json-params
             wrap-debug))

;; (selmer.parser/cache-on!)
;; (selmer.parser/cache-off!)

(mount/defstate http-server
  :start (run-undertow app {:host (:host config/env "0.0.0.0")
                            :port (:port config/env 9090)})
  :stop (.stop http-server))

(defn -main []
  (mount/start))
