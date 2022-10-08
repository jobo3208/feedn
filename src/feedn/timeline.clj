(ns feedn.timeline
  (:require [clojure.set :refer [intersection union]]
            [feedn.state :refer [item-guid-to-path-map state*]]))

(defn- merge-ctx [state item]
  "Merge context from sub, tags, seen, etc. into item"
  (let [{:keys [source channel]} item
        sub (get-in state [:subs [source channel]])
        sub-ctx (dissoc sub :items)
        sub-tags (get sub :tags #{})
        item-tags (get item :tags #{})
        tags (union sub-tags item-tags)
        get-tag-ctx (fn [tag]
                      (-> (get-in state [:tags tag] {})
                          (dissoc :emoji)))
        tags-ctx (apply merge (map get-tag-ctx tags))
        seen-ctx {:seen? (contains? (:seen-items state) (:guid item))}]
    (merge tags-ctx sub-ctx item seen-ctx)))

(defn get-timeline []
  "Get all items from all subs"
  (let [state @state*]
    (->> (-> state :subs vals)
         (mapcat :items)
         (map (partial merge-ctx state))
         (sort-by (juxt (comp not :seen?) :pub-date))
         (reverse))))

(defn mark-seen! [items]
  "Mark items as seen"
  (let [guids (map :guid items)]
    (swap! state* update :seen-items #(into % guids))))

(defn prune-seen [state]
  (let [{:keys [seen-items]} state
        current-guids (into #{} (keys (item-guid-to-path-map state)))]
    (assoc state :seen-items (intersection seen-items current-guids))))
