(ns feedn.limit
  (:require [java-time :as jt]))

(def UPDATES_PER_DAY 24)

(defn- midnight-today []
  (jt/local-date-time (jt/local-date) (jt/local-time 0)))

(defn window-closed? [state]
  (jt/after? (midnight-today) (:limit/window-start state)))

(defn reset-limit [state]
  (assoc state :limit/window-start (midnight-today)
               :limit/updates-remaining (get state :limit/updates-per-day UPDATES_PER_DAY)
               :limit/cutoff-time (or (:limit/cutoff-time state) (jt/instant))))

(defn register-update [state]
  (let [state (if (window-closed? state)
                (reset-limit state)
                state)
        state (if (pos? (:limit/updates-remaining state))
                (-> state
                    (update :limit/updates-remaining dec)
                    (assoc :limit/cutoff-time (jt/instant)))
                state)]
    state))

(defn after-cutoff? [state item]
  (jt/after? (:pub-date item) (:limit/cutoff-time state)))

#_ (swap! feedn.state/state* reset-limit)
