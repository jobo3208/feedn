(ns feedn.timeline-test
  (:require [clojure.test :refer :all]
            [feedn.timeline :as t]))

(def test-config
  {:subs
   {:color "#ddd"
    :sources
    {:nitter
     {:color "#eee"
      :channels
      {"jack"
       {:color "#fff"
        :tags #{:news}}
       "jane"
       {:tags #{:news}}}}}}
   :tags
   {:news
    {:color "#ccc"}}})

(deftest merge-ctx-test
  (are [color item] (= color (:color (t/merge-ctx test-config item)))
    "#ddd" {}  ; root config
    "#eee" {:source :nitter :channel "john"}  ; source config
    "#ccc" {:source :nitter :channel "jane"}  ; tag config
    "#fff" {:source :nitter :channel "jack"}  ; channel config
    "#bbb" {:source :nitter :channel "jack" :color "#bbb"}))  ; item config
