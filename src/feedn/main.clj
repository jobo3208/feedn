(ns feedn.main
  (:require [feedn.config :refer [load-config!]]
            [feedn.frontend :refer [run-server!]]
            [feedn.updater :refer [run-updater!]]
            [clojure.tools.cli :refer [parse-opts]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [nrepl.server]
            [taoensso.timbre :as log]))

(System/setProperty "sun.net.client.defaultConnectTimeout" "5000")
(System/setProperty "sun.net.client.defaultReadTimeout" "5000")

(def cli-options
  [["-c" "--config CONFIG" "Config filepath"
    :default "config.edn"]
   ["-p" "--nrepl-port PORT" "port on which to run optional nrepl server"
    :parse-fn #(Integer/parseInt %)]])

(defn -main [& args]
  (let [opts (:options (parse-opts args cli-options))]
    (do
      (log/merge-config! {:appenders {:spit (log/spit-appender {:fname "log"})}
                          :min-level :info})
      (load-config! (:config opts))
      (when (:nrepl-port opts)
        (log/info (str "Starting nrepl server on port " (:nrepl-port opts)))
        (nrepl.server/start-server :port (:nrepl-port opts) :handler cider-nrepl-handler))
      (future (run-updater!))
      (run-server!))))
