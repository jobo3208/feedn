(ns feedn.source.invidious
  (:require [feedn.source :refer [fetch-items render-item-body]]
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

(defn- parse [source channel opts doc]
  (let [entries (xml/select doc [:entry])
        items (map parse-item entries)
        items (map #(assoc % :source source :channel channel) items)]
     items))

(defmethod fetch-items :invidious
  ([source channel]
   (fetch-items source channel {}))
  ([source channel opts]
   (let [url (java.net.URL. (str "https://yewtu.be/feed/channel/" channel))
         doc (try
               (xml/xml-resource url)
               (catch Exception e
                 (throw (ex-info "fetch error" {:type :fetch :url url} e))))
         items (try
                 (parse source channel opts doc)
                 (catch Exception e
                   (throw (ex-info "parse error" {:type :parse} e))))]
     items)))

(defmethod render-item-body [:html :invidious]
  [_ item]
  (html
    [:div.item-body
     [:h3 {:id (:guid item)} (:author item)]
     [:p [:a {:href (:link item)} (:title item)]]]))
