(ns feedn.source.nitter
  (:require [clojure.string :as string]
            [feedn.source :refer [fetch-items render-item]]
            [feedn.util :refer [ago-str select-text]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(defn- parse-item [name handle item]
  {:title (select-text item [:title])
   :content (select-text item [:description])
   :author handle
   :link (select-text item [:link])
   :guid (select-text item [:guid])
   :pub-date (jt/instant (jt/formatter :rfc-1123-date-time)
                         (select-text item [:pubDate]))
   :nitter/account-name name
   :nitter/account-handle handle
   :nitter/creator (select-text item [:dc:creator])
   :nitter/retweet? (string/starts-with? (select-text item [:title]) "RT by")})

(defmethod fetch-items :nitter
  ([source channel]
   (fetch-items source channel {}))
  ([source channel opts]
   (let [doc (xml/xml-resource (java.net.URL. (str "https://nitter.42l.fr/" channel "/rss")))
         byline (select-text doc [:channel :> :title])
         [_ name handle] (re-matches #"(.+) / (@.+)$" byline)
         items (->> (xml/select doc [:item])
                    (map #(parse-item name handle %))
                    (map #(assoc % :source source :channel channel)))]
     items)))

(defn render-item-footer-html [item]
  (html
    [:div.item-footer
     [:p (ago-str (:pub-date item))]
     [:a {:href (:link item)} "link"]]))

(defmethod render-item [:html :nitter]
  [_ item]
  (html
    [:div {:style (str "background-color: " (:color item))
           :class (if (not (:seen? item))
                    "item unseen"
                    "item")}
     [:h3 (:nitter/account-name item) " (" (:nitter/account-handle item) ")"]
     (when (:nitter/retweet? item)
       [:h4 "RT " (:nitter/creator item)])
     (:content item)
     (render-item-footer-html item)]))
