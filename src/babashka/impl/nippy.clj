(ns babashka.impl.nippy
  {:no-doc true}
  (:require [sci.impl.namespaces :refer [copy-var]]
            [sci.impl.vars :as vars]
            [taoensso.nippy :as nippy]))

(def tns (vars/->SciNamespace 'taoensso.nippy nil))

(def nippy-namespace
  {'freeze (copy-var nippy/freeze tns)
   'thaw (copy-var nippy/thaw tns)})
