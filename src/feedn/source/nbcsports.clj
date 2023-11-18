(ns feedn.source.nbcsports
  (:require [clojure.string :as string]
            [feedn.source.common :refer [prepare-for-html-render]]
            [feedn.source.interface :refer [fetch-items]]
            [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(def channel-urls
  {"NFL Headlines" (java.net.URL. "https://www.nbcsports.com/fantasy/football?f0=Headline&f1=Positions")})

(def uuid-re #"playerNewsId=([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")

(defn- parse-sidebar-item [item]
  (let [link (-> (xml/select item [:a.Link]) first)]
    {:id (second (re-find uuid-re (-> link :attrs :href)))
     :link (-> link :attrs :href)
     :title (xml/text link)}))

(defn- parse-carousel-item [item]
  (let [link (-> (xml/select item [:a.Link]) first)]
    {:id (second (re-find uuid-re (-> link :attrs :href)))
     :nbcsports/news (xml/text link)
     :nbcsports/analysis (-> (xml/select item [:.PlayerNewsCard-analysis]) first xml/text)
     :pub-date (-> (xml/select item [:.PlayerNewsCard-date]) first :attrs :data-date jt/instant)}))

(defn- parse [source channel doc]
  (let [sidebar-items (xml/select doc [:.PlayerNewsModuleSidebar-items-item])
        carousel-items (take 5 (xml/select doc [:.PlayerNewsModuleCarousel-slide]))
        parsed-sidebar-items (map parse-sidebar-item sidebar-items)
        parsed-carousel-items (map parse-carousel-item carousel-items)]
    (map merge parsed-sidebar-items parsed-carousel-items)))

(defmethod fetch-items :nbcsports
  [source channel sub-config]
  (let [url (or (channel-urls channel) (throw (ex-info "no such channel" {})))
        doc (try
              (xml/html-resource url)
              (catch Exception e
                (throw (ex-info "fetch error" {:type :fetch :url url} e))))
        items (try
                (parse source channel doc)
                (catch Exception e
                  (throw (ex-info "parse error" {:type :parse} e))))]
    items))

(defmethod prepare-for-html-render :nbcsports
  [item]
  (assoc item
         :render.html/content
         (str (:nbcsports/news item)
              "<br><br>"
              (:nbcsports/analysis item))))
