(ns feedn.source.rotoworld
  (:require [cheshire.core :as json]
            [feedn.source.common :refer [prepare-for-html-render]]
            [feedn.source.interface :refer [fetch-items]]
            [feedn.util :refer [ago-str]]
            [hiccup.core :refer [html]]
            [java-time :as jt]))

(defn- parse-item [item]
  {:title (get-in item ["attributes" "headline"])
   :content (get-in item ["attributes" "news" "processed"])
   :author (get-in item ["attributes" "source"])
   :link (str "https://www.nbcsportsedge.com/football/nfl/player-news/" (get-in item ["attributes" "drupal_internal__id"]))
   :id (item "id")
   :pub-date (jt/instant (jt/formatter :iso-offset-date-time)
                         (get-in item ["attributes" "created"]))
   :rotoworld/news (get-in item ["attributes" "news" "processed"])
   :rotoworld/analysis (get-in item ["attributes" "analysis" "processed"])
   :rotoworld/source (get-in item ["attributes" "source"])
   :rotoworld/source-url (get-in item ["attributes" "source_url"])
   :rotoworld/injury (get-in item ["attributes" "injury"])
   :rotoworld/transaction (get-in item ["attributes" "transaction"])
   :rotoworld/rumor (get-in item ["attributes" "rumor"])})

(defn- parse [source channel doc]
  (let [source-items (-> doc
                         (json/parse-string)
                         (get "data"))]
     (->> source-items
          (map parse-item))))

(defmethod fetch-items :rotoworld
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

(defmethod prepare-for-html-render :rotoworld
  [item]
  (assoc item
         :render.html/content
         (str (:rotoworld/news item)
              (:rotoworld/analysis item))))
