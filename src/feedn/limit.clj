(ns feedn.limit
  (:require [java-time :as jt]))

(defn- midnight-today []
  (jt/local-date-time (jt/local-date) (jt/local-time 0)))

(defn window-closed? [{:keys [limit-window-start] :as state}]
  (or (nil? limit-window-start)
      (jt/after? (midnight-today) limit-window-start)))

(defn reset-limit [state config]
  (assoc state :limit-window-start (midnight-today)
               :updates-remaining (:updates-remaining config)
               :limit-cutoff-time (or (:limit-cutoff-time state) (jt/instant))))

(defn register-update [state config]
  (let [state (if (window-closed? state)
                (reset-limit state config)
                state)
        state (if (pos? (:updates-remaining state))
                (-> state
                    (update :updates-remaining dec)
                    (assoc :limit-cutoff-time (jt/instant)))
                state)]
    state))

(defn after-cutoff? [{:keys [limit-cutoff-time] :as state} item]
  (or (nil? limit-cutoff-time)
      (jt/after? (:pub-date item) limit-cutoff-time)))
