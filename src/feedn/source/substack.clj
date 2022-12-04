(ns feedn.source.substack
  (:require [clojure.string :as string]
            [feedn.source.common :refer [prepare-for-html-render]]
            [feedn.source.interface :refer [fetch-items]]
            [feedn.util :refer [select-text]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(defn- parse-item [name item]
  {:title (select-text item [:title])
   :content (select-text item [:description])
   :author (select-text item [:dc:creator])
   :link (select-text item [:link])
   :id (select-text item [:guid])
   :pub-date (jt/instant (jt/formatter :rfc-1123-date-time)
                         (select-text item [:pubDate]))
   :substack/newsletter-name name})

(defn- parse [doc]
  (let [name (string/trim (select-text doc [:channel :> :title]))
        items (->> (xml/select doc [:item])
                   (map #(parse-item name %)))]
    items))

(defmethod fetch-items :substack
  [source channel sub-config]
  (let [url-str (or (:url sub-config) (str "https://" channel ".substack.com/feed"))
        url (java.net.URL. url-str)
        doc (try
              (xml/xml-resource url)
              (catch Exception e
                (throw (ex-info "fetch error" {:type :fetch :url url} e))))
        items (try
                (parse doc)
                (catch Exception e
                  (throw (ex-info "parse error" {:type :parse} e))))]
    items))

(defmethod prepare-for-html-render :substack
  [item]
  (assoc item
         :render.html/heading
         (:substack/newsletter-name item)
         :render.html/content
         [:p (:content item)]))
