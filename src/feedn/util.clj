(ns feedn.util
  (:require [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(defn index-by
  "inspired by <https://clojuredocs.org/clojure.core/juxt#example-542692cfc026201cdc326e12>"
  ([key-fn coll]
   (index-by key-fn identity coll))
  ([key-fn val-fn coll]
   (into {} (map (juxt key-fn val-fn) coll))))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn select-text
  "Return the text contained within (xml/select node selector)"
  [node selector]
  (first (xml/select node (conj selector xml/text))))

(defn approx-duration
  "Return an approximation of duration as [unit amount]"
  [duration]
  (let [units [:weeks :days :hours :minutes :seconds]
        amounts (apply jt/as duration units)
        [unit amount] (first (filter #(-> % second pos?) (map vector units amounts)))]
    (if (some? unit)
      [unit amount]
      [:seconds 0])))

(defn human-duration-str
  "Return a human-readable string representation of a duration"
  [duration]
  (let [[unit amount] (approx-duration duration)
        unit-name (name unit)
        unit-name (if (= amount 1)
                    (subs unit-name 0 (dec (count unit-name)))
                    unit-name)]
    (str amount " " unit-name)))

(defn short-human-duration-str
  "Return a short human-readable string representation of a duration"
  [duration]
  (let [[unit amount] (approx-duration duration)
        unit-name (first (name unit))]
    (str amount unit-name)))

(defn ago-str
  "Return a human-readable string representation of the duration from inst to now"
  [inst]
  (str (human-duration-str (jt/duration inst (jt/instant))) " ago"))

(defn short-ago-str
  "Return a short human-readable string representation of the duration from inst to now"
  [inst]
  (short-human-duration-str (jt/duration inst (jt/instant))))
