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

(def conch-namespace
  {;; main API
   'execute conch/execute
   'programs (with-meta programs
               {:sci/macro true})})
