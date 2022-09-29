(ns feedn.timeline
  (:require [feedn.state :refer [state*]]))

(defn- get-items-with-sub-ctx [sub]
  "Get items from sub, merging sub properties into each item"
  (let [items (:items sub)
        sub-ctx (dissoc sub :items)]
    (map #(merge sub-ctx %) items)))

(defn get-timeline []
  "Get all items from all subs"
  (let [state @state*]
    (->> (-> state :subs vals)
         (mapcat get-items-with-sub-ctx)
         (map #(assoc % :seen? (contains? (:seen-items state) (:guid %))))
         (sort-by (juxt (comp not :seen?) :pub-date))
         (reverse))))

(defn mark-seen! [items]
  "Mark items as seen"
  (let [guids (map :guid items)]
    (swap! state* update :seen-items #(into % guids))))
