(ns babashka.impl.nippy
  {:no-doc true}
  (:require [sci.impl.namespaces :refer [copy-var]]
            [sci.impl.vars :as vars]
            [taoensso.nippy :as nippy]))

(def tns (vars/->SciNamespace 'taoensso.nippy nil))

(def nippy-namespace
  {'freeze (copy-var nippy/freeze tns)
   'freeze-to-file (copy-var nippy/freeze-to-file tns)
   'thaw (copy-var nippy/thaw tns)
   'thaw-from-file (copy-var nippy/thaw-from-file tns)})
