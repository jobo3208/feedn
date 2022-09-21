(ns feedn.source.rotoworld
  (:require [cheshire.core :as json]
            [feedn.source :refer [fetch-items render-item render-item-footer-html]]
            [feedn.util :refer [ago-str]]
            [hiccup.core :refer [html]]
            [java-time :as jt]))

(defn- parse-item [item]
  {:title (get-in item ["attributes" "headline"])
   :content (get-in item ["attributes" "news" "processed"])
   :author (get-in item ["attributes" "source"])
   :guid (item "id")
   :pub-date (jt/instant (jt/formatter :iso-offset-date-time)
                         (get-in item ["attributes" "created"]))
   :rotoworld/news (get-in item ["attributes" "news" "processed"])
   :rotoworld/analysis (get-in item ["attributes" "analysis" "processed"])
   :rotoworld/source (get-in item ["attributes" "source"])
   :rotoworld/source-url (get-in item ["attributes" "source_url"])
   :rotoworld/injury (get-in item ["attributes" "injury"])
   :rotoworld/transaction (get-in item ["attributes" "transaction"])
   :rotoworld/rumor (get-in item ["attributes" "rumor"])})

(defmethod fetch-items :rotoworld
  ([source channel]
   (fetch-items source channel {}))
  ([source channel opts]
   (let [url channel
         items (-> (slurp url)
                  (json/parse-string)
                  (get "data"))]
     (->> items
          (map parse-item)
          (map #(assoc % :source source :channel channel))))))

(defmethod render-item [:html :rotoworld]
  [_ item]
  (html
    [:div {:style (str "background-color: " (:color item))
           :class (if (not (:seen? item))
                    "item unseen"
                    "item")}
     [:h3 (:title item)]
     (:rotoworld/news item)
     (:rotoworld/analysis item)
     (render-item-footer-html item)]))
