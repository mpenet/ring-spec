(ns qbits.ring-spec
  "Playing with spec and ring"
  (:require
   [qbits.spex :as x]
   [qbits.spex.networking :as xn]
   [ring.core.protocols]
   [clojure.java.io :as io]
   [clojure.spec :as s]
   [clojure.spec.gen :as g]
   [clojure.test.check.generators :as gen]))


(def input-stream-gen (gen/fmap io/input-stream gen/bytes))

(create-ns 'ring.spec)

;; REQUEST

(x/ns-as 'ring.spec.request 'request)

;; required
(s/def ::request/server-port ::xn/port)
(s/def ::request/server-name ::xn/hostname)
(s/def ::request/remote-addr ::xn/ip)
(s/def ::request/uri ::xn/uri)
(s/def ::request/headers (s/map-of string? string?))


;; opt
(s/def ::request/query-string string?)
(s/def ::request/scheme #{:https :http})
(s/def ::request/request-method (s/or :method #{:get :post :put :delete :option
                                                :patch :head :trace :connect}
                                      :extension-method keyword?))

(s/def ::request/protocol string?)
(s/def ::request/ssl-client-cert
  (x/instance-of java.security.cert.X509Certificate))
(s/def ::request/body (s/spec (x/instance-of java.io.InputStream)
                              :gen (constantly input-stream-gen)))

;; deprecated
(s/def ::request/content-type string?)
(s/def ::request/content-length int?)
(s/def ::request/character-encoding string?)

(s/def :ring.spec/request
  (s/keys :req-un [::request/server-name
                   ::request/server-port
                   ::request/remote-addr
                   ::request/uri
                   ::request/headers]
          :opt-un [
                   ::request/query-string
                   ::request/scheme
                   ::request/request-method
                   ::request/protocol
                   ;; ::request/ssl-client-cert
                   ::request/body
                   ::request/content-type
                   ::request/content-length
                   ::request/character-encoding]))


;; RESPONSE
(x/ns-as 'ring.spec.response 'response)
(s/def ::response/status (s/and pos-int? #(s/int-in-range? 100 599 %)))
(s/def ::response/headers (s/map-of string?
                                    (s/or :value string?
                                          :values (s/coll-of string?))))
(s/def ::response/body (s/spec (x/satisfies ring.core.protocols/StreamableResponseBody)
                               :gen (fn []
                                      (gen/one-of [gen/string gen/bytes
                                                   (gen/list gen/string)
                                                   input-stream-gen
                                                   (gen/return nil)]))))
(s/def :ring.spec/response
  (s/keys :req-un [::response/status ::response/headers]
          :opt-un [::response/body]))

(x/ns-as 'ring.spec.response.handler 'handler)

;; A synchronous handler takes 1 argument, a request map, and returns a response
(s/def ::handler/sync
  (s/fspec :args (s/cat :request :ring.spec/request)
           :ret :ring.spec/response))

;; An asynchronous handler takes 3 arguments: a request map, a callback function
;; for sending a response and a callback function for raising an exception. The
;; response callback takes a response map as its argument. The exception callback
;; takes an exception as its argument.
(s/def ::handler/response-callback
  (s/fspec :args (s/cat :request :ring.spec/request)
           :ret any?))

(s/def ::handler/error-callback
  (s/fspec :args (s/cat :error
                        (s/spec (x/instance-of Exception)
                                :gen (fn [] (gen/fmap #(Exception. (str %))
                                                      gen/string))))
           :ret any?))

(s/def ::handler/async
  (s/fspec :args (s/cat :request :ring.spec/request
                        :response-callback ::handler/response-callback
                        :error-callback ::handler/error-callback )
           :ret any?))

(s/def ::ring.spec/handler
  (s/or :sync-handler ::handler/sync
        :async-handler ::handler/async))

;; FUN
;; (binding [s/*fspec-iterations* 1
;;           s/*recursion-limit* 1]
;;   (prn "---------------------------------------------")
;;   ;; (prn (first (g/sample (s/gen ::ring.spec/handler))))
;;   (prn ((first (g/sample (s/gen ::ring.spec/handler))
;;                ;; (first (g/sample (s/gen ::ring.spec/request)))
;;                )
;;         {:server-port 100 :server-name "asdfadf" :uri "http://asdfaf"
;;          :headers {}
;;          :remote-addr "111.111.111.111"})))




;; (s/def ::ring.spec/middleware )
;; (s/def ::ring.spec/adapter)
