(ns feedn.source
  (:require [feedn.state :refer [state*]]
            [feedn.util :refer [short-ago-str]]
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

(def channel-emoji "\uD83D\uDC64")

(def tag-emoji "\uD83C\uDFF7")

(defn tag-link [tag]
  [:a {:href (str "?tag=" (name tag))
       :class :emoji-link} (get-in @state* [:tags tag :emoji] tag-emoji)])

(defn render-item-footer-html [item]
  (html
    [:div.item-footer
     (short-ago-str (:pub-date item))
     " | "
     [:a {:href (:link item) :class :emoji-link} link-emoji]
     " | "
     [:a {:href (str "?source=" (name (:source item)) "&channel=" (:channel item))
          :class :emoji-link} channel-emoji]
     " "
     (interpose " " (map tag-link (:tags item)))]))

(require 'feedn.source.nitter)
(require 'feedn.source.rotoworld)
(require 'feedn.source.invidious)
