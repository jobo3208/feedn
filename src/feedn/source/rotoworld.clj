(ns feedn.source.rotoworld
  (:require [cheshire.core :as json]
            [feedn.source.interface :refer [fetch-items render-item-body]]
            [feedn.util :refer [ago-str]]
            [hiccup.core :refer [html]]
            [java-time :as jt]))

(defn- parse-item [item]
  {:title (get-in item ["attributes" "headline"])
   :content (get-in item ["attributes" "news" "processed"])
   :author (get-in item ["attributes" "source"])
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

(defmethod render-item-body [:html :rotoworld]
  [_ item]
  (html
    [:div.item-body
     [:h3 {:id (:guid item)} (:title item)]
     (:rotoworld/news item)
     (:rotoworld/analysis item)]))
