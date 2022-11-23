(ns feedn.source.common
  (:require [feedn.config :refer [config_]]
            [feedn.source.interface :refer [render-item render-item-body]]
            [feedn.util :refer [short-ago-str]]
            [hiccup.core :refer [html]]))

(def link-emoji "\uD83D\uDD17")

(def channel-emoji "\uD83D\uDC64")

(def tag-emoji "\uD83C\uDFF7")

(defn tag-link [tag]
  [:a {:href (str "?tag=" (name tag))
       :class :emoji-link} (get-in @config_ [:tags tag :emoji] tag-emoji)])

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

(defmethod render-item :html
  [_ item]
  (html
    [:div {:style (str "background-color: " (:color item))
           :class (if (not (:seen? item))
                    "item unseen"
                    "item")}
     (render-item-body :html item)
     (render-item-footer-html item)]))
