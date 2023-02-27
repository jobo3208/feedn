(ns feedn.main
  (:require [feedn.config :refer [load-config!]]
            [feedn.frontend :refer [run-server!]]
            [feedn.updater :refer [run-updater!]]
            [taoensso.timbre :as log]))

(System/setProperty "sun.net.client.defaultConnectTimeout" "5000")
(System/setProperty "sun.net.client.defaultReadTimeout" "5000")

(defn -main
  ([]
   (-main "config.edn"))
  ([config-filepath]
   (log/merge-config! {:appenders {:spit (log/spit-appender {:fname "log"})}
                       :min-level :info})
   (load-config! config-filepath)
   (future (run-updater!))
   (run-server!)))
