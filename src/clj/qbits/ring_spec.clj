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


(def input-stream-gen
  (gen/fmap io/input-stream gen/bytes))

(create-ns 'ring.spec)

;; REQUEST

(x/ns-as 'ring.spec.request 'request)

;; required
(s/def ::request/server-port ::xn/port)
(s/def ::request/server-name ::xn/hostname)
(s/def ::request/remote-addr ::xn/ip)
(s/def ::request/uri uri?)
(s/def ::request/headers (s/map-of string? string?))

;; (prn ::request/headers)

;; opt
(s/def ::request/query-string string?)
(s/def ::request/scheme #{:https :http})
(s/def ::request/request-method (s/or
                                 :method #{:get :post :put :delete :option
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
  (s/keys
   :req-un [::request/server-name
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
  (s/keys
   :req-un [::response/status ::response/headers]
   :opt-un [::response/body]))

;; (s/exercise :ring.spec/request)
;; (s/exercise :ring.spec/response)

(s/def ::ring.spec/handler
  (s/fspec
   :args (s/or :sync (s/cat :request :ring.spec/request)
               :async (s/cat :request :ring.spec/request
                             :async-response
                             (s/fspec
                              :args (s/cat :request :ring.spec/request)
                              :ret :ring.spec/response)
                             :async-error
                             (s/fspec
                              :args (s/cat :error
                                           (s/spec (x/instance-of Exception)
                                                   :gen (fn [] (gen/fmap #(Exception. (str %))
                                                                         gen/string))))
                              :ret any?)))
   :ret :ring.spec/response))


;; FUN
(binding [s/*fspec-iterations* 1]
  (prn ((first (g/sample (s/gen ::ring.spec/handler)) )
        (first (g/sample (s/gen ::ring.spec/request))))))




;; (s/def ::ring.spec/middleware )
;; (s/def ::ring.spec/adapter)
