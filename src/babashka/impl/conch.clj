(ns babashka.impl.conch
  {:no-doc true}
  (:require
   [babashka.impl.me.raynes.conch.low-level :as ll]
   [babashka.impl.me.raynes.conch :as conch]))

(def conch-ll-namespace
  {;; low level API
   'proc ll/proc
   'destroy ll/destroy
   'exit-code ll/exit-code
   'flush ll/flush
   'done ll/done
   'stream-to ll/stream-to
   'feed-from ll/feed-from
   'stream-to-string ll/stream-to-string
   'stream-to-out ll/stream-to-out
   'feed-from-string ll/feed-from-string
   'read-line ll/read-line})

(defn programs
  "Creates functions corresponding to progams on the PATH, named by names."
  [_ _ & names]
  `(do ~@(for [name names]
           `(defn ~name [& ~'args]
              (apply ~'me.raynes.conch/execute ~(str name) ~'args)))))

(defn program-form [prog]
  `(fn [& args#] (apply ~'me.raynes.conch/execute ~prog args#)))

(defn let-programs
  [_ _ bindings & body]
  `(let [~@(conch/map-nth #(program-form %) 1 2 bindings)]
     ~@body))

(defn with-programs
  [_ _ programs & body]
  `(let [~@(interleave programs (map (comp program-form str) programs))]
     ~@body))

(def conch-namespace
  {;; main API
   'execute conch/execute
   'let-programs (with-meta let-programs
                    {:sci/macro true})
   'with-programs (with-meta with-programs
                    {:sci/macro true})
   'programs (with-meta programs
               {:sci/macro true})})
