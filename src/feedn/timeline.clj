(ns feedn.timeline
  (:require [feedn.state :refer [state*]]))

(defn get-timeline []
  (->> (-> @state* :subs vals)
       (mapcat :items)
       (sort-by :pub-date)
       (reverse)))
