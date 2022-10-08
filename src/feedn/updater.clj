(ns feedn.updater
  (:require [feedn.source :refer [fetch-items]]
            [feedn.state :refer [state*]]
            [feedn.util :refer [index-by]]
            [java-time :as jt]))

(defn- tick-sub [{:keys [period timer] :as sub}]
  "Advance timer for sub"
  (let [timer (case timer
                nil (rand-int period)
                0 period
                (dec timer))]
    (assoc sub :timer timer)))

(defn- tick-subs [state]
  "Advance timers for all subs"
  (update state :subs #(update-vals % tick-sub)))

(defn- new-items-are-stale? [sub items]
  "Return true if items are older than the items in the sub (happens w/ certain nitter instances sometimes)"
  (and (seq (:items sub))
       (seq items)
       (jt/after?
         (apply jt/max (map :pub-date (:items sub)))
         (apply jt/max (map :pub-date items)))))

(defn- update-items [sub items]
  "Add items to sub by merging new into old. Old items that are not in the passed collection are removed."
  (if (new-items-are-stale? sub items)
    sub
    (let [existing-items (get sub :items [])
          existing-items-by-guid (index-by :guid existing-items)
          items (mapv #(merge (get existing-items-by-guid (:guid %) {}) %) items)]
      (assoc sub :items items))))

(defn- fetch-and-update! [[source channel]]
  "Fetch latest items for [source channel] and add to state"
  (let [items (fetch-items source channel)]
    (swap! state* update-in [:subs [source channel]] #(-> %
                                                          (update-items items)
                                                          (assoc :last-fetched (jt/instant))))))

(defn run-updater! []
  "Run updater, which will fetch items from all subs according to period"
  (loop []
    (let [state (swap! state* tick-subs)
          to-fetch (->> (:subs state)
                        (filter (fn [[k v]] (= 0 (:timer v))))
                        (keys))]
      (dorun (map #(future (fetch-and-update! %)) to-fetch))
      (Thread/sleep 1000)
      (recur))))

#_ (tick-subs @state*)

#_ (-> @state*
       :subs
       (get [:nitter "ckparrot"])
       (update-items [{:title "hai" :guid "123" :seen? true}])
       (update-items [{:title "bai" :guid "123"}])
       (update-items [{:title "bai" :guid "123"} {:title "yeah" :guid "456"}])
       (update-items [{:title "yeah" :guid "456"}]))

#_ (def updater (future (run-updater!)))

#_ (future-cancel updater)

#_ (-> @state*
       :subs
       (get [:nitter "ckparrot"]))
