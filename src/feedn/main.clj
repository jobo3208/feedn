(ns feedn.main
  (:require [feedn.config :refer [load-config]]
            [feedn.frontend :refer [run-server!]]
            [feedn.limit :refer [reset-limit]]
            [feedn.state :refer [state*]]
            [feedn.updater :refer [run-updater!]]))

(set! *print-length* 10)

(defn -main [& args]
  (System/setProperty "sun.net.client.defaultConnectTimeout" "5000")
  (System/setProperty "sun.net.client.defaultReadTimeout" "5000")
  (let [config (load-config "config.edn")]
    (swap! state* merge config)
    (swap! state* reset-limit)
    (future (run-updater!))
    (run-server!)))

#_ (-main)

#_ (deref state*)
