(ns feedn.source.invidious
  (:require [feedn.source :refer [fetch-items render-item render-item-footer-html]]
            [feedn.util :refer [ago-str select-text]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(defn- parse-item [item]
  {:title (select-text item [:title])
   :author (select-text item [:author :name])
   :link (-> (xml/select item [:link]) first :attrs :href)
   :guid (select-text item [:id])
   :pub-date (jt/instant (jt/formatter :iso-offset-date-time)
                         (select-text item [:published]))})

(defmethod fetch-items :invidious
  ([source channel]
   (fetch-items source channel {}))
  ([source channel opts]
   (let [doc (xml/xml-resource (java.net.URL. (str "https://yewtu.be/feed/channel/" channel)))
         entries (xml/select doc [:entry])
         items (map parse-item entries)
         items (map #(assoc % :source source :channel channel) items)]
     items)))

(defmethod render-item [:html :invidious]
  [_ item]
  (html
    [:div {:style (str "background-color: " (:color item))
           :class (if (not (:seen? item))
                    "item unseen"
                    "item")}
     [:h3 (:author item)]
     [:p [:a {:href (:link item)} (:title item)]]
     (render-item-footer-html item)]))
