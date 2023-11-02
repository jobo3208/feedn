(ns feedn.source.fantasypros
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [feedn.source.common :refer [prepare-for-html-render]]
            [feedn.source.interface :refer [fetch-items]]
            [java-time :as jt]))

(defn- parse-item [item]
  {:title (get item "title")
   :content (get item "content")
   :author (get item "author")
   :link (get item "url")
   :id (get item "news_id")
   :pub-date (jt/instant (* 1000 (get item "published_timestamp")))
   :fantasypros/quote (get item "quote")
   :source :fantasypros})

(defn- parse [source channel doc]
  (let [news-line (->> (string/split-lines doc)
                       (map string/trim)
                       (filter #(string/starts-with? % "var news ="))
                       (first))
        json-str (subs news-line 11 (dec (count news-line)))
        data (json/parse-string json-str)
        source-items (get data "news")]
     (->> source-items
          (map parse-item))))

(defmethod fetch-items :fantasypros
  [source channel sub-config]
  (let [url (:url sub-config)
        doc (try
              (slurp url)
              (catch Exception e
                (throw (ex-info "fetch error" {:type :fetch :url url} e))))
        items (try
                (parse source channel doc)
                (catch Exception e
                  (throw (ex-info "parse error" {:type :parse} e))))]
    items))

(defmethod prepare-for-html-render :fantasypros
  [item]
  (assoc item
         :render.html/content
         (str (:fantasypros/quote item) "<br><br>" (:content item))))
