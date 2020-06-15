(ns babashka.impl.clj-http
  {:no-doc true}
  (:require [clj-http.client :as client]
            [sci.impl.namespaces :refer [copy-var]]
            [sci.impl.vars :as vars]))

(def tns (vars/->SciNamespace 'clj-http.client nil))

(def clj-http-client-namespace
  {'head (copy-var client/head tns)
   'get (copy-var client/get tns)
   'post (copy-var client/post tns)
   'put (copy-var client/put tns)
   'delete (copy-var client/delete tns)})
