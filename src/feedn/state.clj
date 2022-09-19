(ns feedn.state)

(def initial-state
  {:subs {}
   :seen-items #{}})

(defonce state* (atom initial-state))
