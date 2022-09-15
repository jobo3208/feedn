(ns feedn.config
  (:refer-clojure :exclude [subs])
  (:require [clojure.edn :as edn]))

(defn- expand-subs
  "Expand subs from condensed config format to full format"
  [subs]
  (->> (apply concat
        (let [root-opts (dissoc subs :sources)]
          (for [[source-name source-data] (:sources subs)]
            (let [source-opts (merge root-opts (dissoc source-data :subs))]
              (for [[channel sub-opts] (:subs source-data)]
                (let [opts (merge source-opts sub-opts)]
                  [[source-name channel] (merge opts {:source source-name
                                                      :channel channel})]))))))
       (into {})))

(defn load-config
  "Load config from filepath and return config map"
  [filepath]
  (-> filepath
      slurp
      edn/read-string
      (update :subs expand-subs)))

#_ (load-config "config.edn")
