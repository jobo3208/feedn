(ns feedn.source.nitter
  (:require [clojure.string :as string]
            [feedn.source.interface :refer [fetch-items render-item-body]]
            [feedn.util :refer [select-text]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(def NITTER-FETCH-DOMAIN "twiiit.com")
(def NITTER-LINK-MEDIA-DOMAIN "nitter.net")

(defn- parse-item [name handle item]
  {:title (select-text item [:title])
   :content (select-text item [:description])
   :author handle
   :link (select-text item [:link])
   :id (->> (select-text item [:guid])
            (re-find #"status/(\d+)")
            second)
   :pub-date (jt/instant (jt/formatter :rfc-1123-date-time)
                         (select-text item [:pubDate]))
   :nitter/account-name name
   :nitter/account-handle handle
   :nitter/creator (select-text item [:dc:creator])
   :nitter/retweet? (string/starts-with? (select-text item [:title]) "RT by")})

(defn- replace-domain [from to v]
  (if (string? v)
    (string/replace v from to)
    v))

(defn- parse [source channel doc]
  (let [byline (select-text doc [:channel :> :title])
        [_ name handle] (re-matches #"(.+) / (@.+)$" byline)
        link (select-text doc [:channel :> :link])
        [_ _ domain _] (string/split link #"/")
        items (->> (xml/select doc [:item])
                   (map #(parse-item name handle %))
                   (map #(update-vals % (partial replace-domain domain NITTER-LINK-MEDIA-DOMAIN)))
                   (map #(assoc % :nitter/domain domain)))]
    items))

(defmethod fetch-items :nitter
  [source channel _]
  (let [url (java.net.URL. (str "https://" NITTER-FETCH-DOMAIN "/" channel "/rss"))
        doc (try
              (xml/xml-resource url)
              (catch Exception e
                (throw (ex-info "fetch error" {:type :fetch :url url} e))))
        items (try
                (parse source channel doc)
                (catch Exception e
                  (throw (ex-info "parse error" {:type :parse} e))))]
    items))

(defmethod render-item-body [:html :nitter]
  [_ item]
  (html
    [:div.item-body
     [:h3 {:class "card-title" :id (:guid item)} (:nitter/account-name item) " (" (:nitter/account-handle item) ")"]
     (when (:nitter/retweet? item)
       [:h4 {:style "card-subtitle"} "RT " (:nitter/creator item)])
     (:content item)]))
