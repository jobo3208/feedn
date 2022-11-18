(ns feedn.config
  (:require [clojure.edn :as edn]
            [feedn.util :refer [deep-merge]]))

(def default-config
  {:subs
   {:period 120
    :max-items 10
    :min-volume 2
    :color "#ddd"}
   :updates-remaining 24
   :volume 2})

(defonce config_ (atom default-config))

(defn load-config [config filepath]
  (deep-merge config (-> filepath slurp edn/read-string)))
