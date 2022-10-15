(ns feedn.main
  (:require [feedn.config :refer [load-config]]
            [feedn.frontend :refer [run-server!]]
            [feedn.limit :refer [reset-limit]]
            [feedn.state :refer [state*]]
            [feedn.updater :refer [run-updater!]]
            [taoensso.timbre :refer [merge-config! spit-appender]]))

(set! *print-length* 10)

(defn -main [& args]
  (System/setProperty "sun.net.client.defaultConnectTimeout" "5000")
  (System/setProperty "sun.net.client.defaultReadTimeout" "5000")
  (merge-config! {:appenders {:spit (spit-appender {:fname "log"})}})
  (let [config (load-config "config.edn")]
    (swap! state* merge config)
    (swap! state* reset-limit)
    (future (run-updater!))
    (run-server!)))

#_ (-main)

#_ (deref state*)
