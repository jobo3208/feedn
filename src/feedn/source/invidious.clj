(ns feedn.source.invidious
  (:require [feedn.source.interface :refer [fetch-items render-item-body]]
            [feedn.util :refer [ago-str select-text]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(defn- parse-item [item]
  {:title (select-text item [:title])
   :author (select-text item [:author :name])
   :link (-> (xml/select item [:link]) first :attrs :href)
   :id (select-text item [:id])
   :pub-date (jt/instant (jt/formatter :iso-offset-date-time)
                         (select-text item [:published]))})

(defn- parse [source channel doc]
  (let [entries (xml/select doc [:entry])
        items (map parse-item entries)]
     items))

(defmethod fetch-items :invidious
  [source channel sub-config]
  (let [channel-id (:invidious/channel-id sub-config)
        url (java.net.URL. (str "https://yewtu.be/feed/channel/" channel-id))
        doc (try
              (xml/xml-resource url)
              (catch Exception e
                (throw (ex-info "fetch error" {:type :fetch :url url} e))))
        items (try
                (parse source channel doc)
                (catch Exception e
                  (throw (ex-info "parse error" {:type :parse} e))))]
    items))

(defmethod render-item-body [:html :invidious]
  [_ item]
  (html
    [:div.item-body
     [:h3 {:id (:guid item)} (:author item)]
     [:p [:a {:href (:link item)} (:title item)]]]))
