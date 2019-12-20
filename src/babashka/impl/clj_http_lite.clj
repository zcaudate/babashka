(ns babashka.impl.clj-http-lite
  (:require [clj-http.lite.client :as client]))

(def client-namespace
  ;; created with cider-pprint of the expression in the comment below
  {'wrap-request client/wrap-request,
   'post client/post,
   'follow-redirect client/follow-redirect,
   'when-pos client/when-pos,
   'wrap-accept-encoding client/wrap-accept-encoding,
   'basic-auth-value client/basic-auth-value,
   'wrap-input-coercion client/wrap-input-coercion,
   'wrap-url client/wrap-url,
   'parse-url client/parse-url,
   'wrap-query-params client/wrap-query-params,
   'wrap-unknown-host client/wrap-unknown-host,
   'unexceptional-status? client/unexceptional-status?,
   ;; 'with-connection-pool client/with-connection-pool,
   'wrap-redirects client/wrap-redirects,
   'wrap-output-coercion client/wrap-output-coercion,
   'head client/head,
   'request client/request,
   'wrap-content-type client/wrap-content-type,
   'put client/put,
   'generate-query-string client/generate-query-string,
   'accept-encoding-value client/accept-encoding-value,
   'wrap-method client/wrap-method,
   'get client/get,
   'content-type-value client/content-type-value,
   'wrap-accept client/wrap-accept,
   'wrap-basic-auth client/wrap-basic-auth,
   'wrap-decompression client/wrap-decompression,
   'wrap-user-info client/wrap-user-info,
   'delete client/delete,
   'parse-user-info client/parse-user-info,
   'update client/update,
   'wrap-exceptions client/wrap-exceptions,
   'wrap-form-params client/wrap-form-params})

(comment
  (into {}
        (for [k (keys (ns-publics 'clj-http.lite.client))]
          [`'~k (symbol "client" (str k))]))
  )
