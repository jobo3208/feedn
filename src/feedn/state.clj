(ns feedn.state)

(def initial-state
  {:subs {}
   :seen-items #{}})

(defonce state* (atom initial-state))

(defn item-guid-to-path-map [state]
  (->> (apply concat
        (for [[[source channel] sub] (:subs state)]
          (for [[i item] (map-indexed vector (:items sub))]
            [(:guid item) [:subs [source channel] :items i]])))
       (into {})))
