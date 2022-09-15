(ns feedn.main
  (:require [feedn.config :refer [load-config]]
            [feedn.frontend :refer [run-server!]]
            [feedn.state :refer [state*]]
            [feedn.updater :refer [run-updater!]]))

(set! *print-length* 10)

(defn -main [& args]
  (let [config (load-config "config.edn")]
    (reset! state* config)
    (future (run-updater!))
    (run-server!)))

#_ (-main)

#_ (deref state*)
