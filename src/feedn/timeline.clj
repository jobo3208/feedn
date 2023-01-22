(ns feedn.timeline
  (:require [clojure.set :refer [union]]
            [feedn.sub :refer [iter-subs]]))

(defn merge-ctx
  "Merge context from root config, source, tags, and channel into item"
  [config item]
  (let [{:keys [source channel]} item
        root-ctx (-> config :subs (dissoc :sources))
        source-ctx (-> config (get-in [:subs :sources source]) (dissoc :channels))
        channel-ctx (-> config (get-in [:subs :sources source :channels channel]))
        channel-tags (get channel-ctx :tags #{})
        item-tags (get item :tags #{})
        tags (union channel-tags item-tags)
        get-tag-ctx (fn [tag]
                      (-> (get-in config [:tags tag] {})
                          (dissoc :emoji)))
        tags-ctx (apply merge (map get-tag-ctx tags))]
    (merge root-ctx source-ctx tags-ctx channel-ctx item)))

(defn get-timeline
  "Get all items from all subs"
  [state config]
  (->> state
       iter-subs
       (map last)
       (mapcat :items)
       (map (partial merge-ctx config))
       (sort-by (juxt (comp not :seen?) :pub-date :nitter/retweet?))
       (reverse)))

(defn item-guid-to-path-map [state]
  (->> state
       iter-subs
       (map (fn [[source channel sub]]
              (for [[i item] (map-indexed vector (:items sub))]
                [(:guid item) [:subs :sources source :channels channel :items i]])))
       (apply concat)
       (into {})))

(defn mark-seen [state guids]
  (let [guid-to-path (item-guid-to-path-map state)
        paths (-> (select-keys guid-to-path guids) vals)]
    (reduce
      (fn [s path]
        (assoc-in s (conj path :seen?) true))
      state
      paths)))
