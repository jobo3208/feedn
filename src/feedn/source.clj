(ns feedn.source
  (:require [feedn.source.interface :as interface]
            feedn.source.common
            feedn.source.dumpor
            feedn.source.invidious
            feedn.source.nitter
            feedn.source.rotoworld))

(defn fetch-items [source channel sub-config]
  (->> (interface/fetch-items source channel sub-config)
       (map #(assoc % :source source
                      :channel channel
                      :guid (hash [source channel (:id %)])))))

(def render-item interface/render-item)
