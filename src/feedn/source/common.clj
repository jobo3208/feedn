(ns feedn.source.common
  (:require [feedn.config :refer [config_]]
            [feedn.source.interface :refer [render-item]]
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

(defmulti prepare-for-html-render :source)

(defmethod prepare-for-html-render :default [item] item)

(defmethod render-item :html
  [_ item]
  (let [item (prepare-for-html-render item)
        heading (or (:render.html/heading item) (:title item))
        subheading (:render.html/subheading item)
        content (or (:render.html/content item) (:content item))]
    (html
      [:div {:style (str "background-color: " (:color item))
             :class (if (not (:seen? item))
                      "item unseen"
                      "item")}
       [:div.item-body
        [:h3 {:class "card-title" :id (:guid item)} heading]
        (when subheading
          [:h4 {:style "card-subtitle"} subheading])
        content]
       (render-item-footer-html item)])))
