(ns feedn.frontend.filter)

(def all-filter-params #{:source :channel :tag})

(defmulti coerce-filter-param (fn [k v] k))
(defmethod coerce-filter-param :source [_ v] (keyword v))
(defmethod coerce-filter-param :tag [_ v] (keyword v))
(defmethod coerce-filter-param :default [_ v] v)

(defn params->filter-params [params]
  (->> (select-keys params all-filter-params)
       (map (fn [[k v]]
              [k (coerce-filter-param k v)]))
       (into {})))

(defmulti filter-param->fn (fn [k v] k))
(defmethod filter-param->fn :tag [_ v] (fn [item] ((:tags item) v)))
(defmethod filter-param->fn :default [k v] (comp (partial = v) k))

(defn filter-params->fn [filter-params]
  (apply every-pred (map #(apply filter-param->fn %) filter-params)))
