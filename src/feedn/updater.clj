(ns feedn.updater
  (:require [feedn.config :refer [config_]]
            [feedn.source :refer [fetch-items]]
            [feedn.state :refer [state_]]
            [feedn.sub :as sub]
            [feedn.util :refer [index-by]]
            [java-time :as jt]
            [taoensso.timbre :as log]))

(defn refresh-subs
  "Add subs from config that are not in state; remove subs from state that are not in config"
  [state config]
  (assoc
    state
    :subs
    (reduce
      (fn [m [s c _]]
        (let [sub-state (or (sub/get-sub-state state s c) {})]
          (assoc-in m [:sources s :channels c] sub-state)))
      {}
      (sub/iter-subs config))))

(defn update-state-from-config [state config]
  (let [state (merge (select-keys config [:updates-remaining :volume]) state)
        state (refresh-subs state config)]
    state))

(defn tick-sub-timer [{:keys [timer] :as sub-state} {:keys [period] :as sub-config}]
  (let [timer (case timer
                nil (rand-int period)
                0 period
                (dec timer))]
    (assoc sub-state :timer timer)))

(defn tick-sub-timers [state config]
  (sub/update-subs
    state
    (fn [s c sub-state]
      (let [sub-config (sub/get-sub-config config s c)]
        (tick-sub-timer sub-state sub-config)))))

(defn new-items-are-stale?
  "Return true if items are older than the items in the sub (happens w/ certain nitter instances sometimes)"
  [sub items]
  (and (seq (:items sub))
       (seq items)
       (jt/after?
         (apply jt/max (map :pub-date (:items sub)))
         (apply jt/max (map :pub-date items)))))

(defn update-items
  "Add items to sub by merging new into old. Old items that are not in the passed collection are removed."
  [sub items]
  (if (new-items-are-stale? sub items)
    sub
    (let [existing-items (get sub :items [])
          existing-items-by-guid (index-by :guid existing-items)
          items (reverse (sort-by :pub-date items))
          items (if (:max-items sub)
                  (take (:max-items sub) items)
                  items)
          items (mapv #(merge (get existing-items-by-guid (:guid %) {}) %) items)]
      (assoc sub :items items))))

(defn fetch-and-update! [source channel]
  (let [[items error]
        (try
          [(fetch-items source channel) nil]
          (catch Exception e
            (case (:type (ex-data e))
              :fetch (log/debug (str (ex-cause e)) [source channel])
              (log/error e [source channel]))
            [nil e]))]
    (swap!
      state_
      update-in
      [:subs :sources source :channels channel]
      (fn [sub]
        (let [now (jt/instant)
              sub (assoc sub :last-fetch-attempt now)]
          (if (some? items)
            (-> sub
                (assoc :last-successful-fetch now)
                (update-items items))
            (assoc sub :last-fetch-error error)))))))

(defn tick-updater! []
  (let [config @config_]
    (swap! state_ update-state-from-config config)
    (swap! state_ tick-sub-timers config)
    (let [state @state_
          subs-to-fetch (->> (sub/iter-subs state)
                             (filter (fn [[_ _ sub-state]]
                                       (= (:timer sub-state) 0))))]
      (dorun
        (map (fn [[source channel _]]
               (future
                 (fetch-and-update! source channel)))
             subs-to-fetch)))))

(defn run-updater! []
  (loop []
    (tick-updater!)
    (Thread/sleep 1000)
    (recur)))

(comment

  (defn trigger-and-tick! []
    (swap! state_ sub/update-subs (fn [_ _ m] (assoc m :timer 1)))
    (tick-updater!))

  (def updater (future (run-updater!))))
