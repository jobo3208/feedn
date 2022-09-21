(ns feedn.source
  (:require [feedn.util :refer [short-ago-str]]
            [hiccup.core :refer [html]]))

(defmulti fetch-items
  "Fetch items from source and return a sequence of items"
  (fn [source channel & opts]
    source))

(defmulti render-item
  "Render an item for display in the given format. Dispatches on format and item's source"
  (fn [fmt item & opts]
    [fmt (:source item)]))

(def link-emoji "\uD83D\uDD17")

(def tag->emoji
  {:football "\uD83C\uDFC8"
   :dolphins "\uD83D\uDC2C"})

(defn render-item-footer-html [item]
  (html
    [:div.item-footer
     [:small (short-ago-str (:pub-date item))
      " | "
      [:a {:href (:link item) :class :emoji-link} link-emoji]
      " | "
      (interpose " " (map tag->emoji (:tags item)))]]))

(require 'feedn.source.nitter)
(require 'feedn.source.rotoworld)
(require 'feedn.source.invidious)
