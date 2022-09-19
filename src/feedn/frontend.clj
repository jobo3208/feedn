(ns feedn.frontend
  (:require [compojure.core :refer [routes GET]]
            [compojure.handler :refer [site]]
            [feedn.source :refer [render-item]]
            [feedn.timeline :refer [get-timeline mark-seen!]]
            [hiccup.core :refer [html]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as resp]))

(defn index& []
  (let [timeline (get-timeline)
        [unseen seen] (split-with (comp not :seen?) timeline)]
    (mark-seen! timeline)
    (html
      [:html
       [:head
        [:link {:rel "stylesheet" :href "style.css"}]]
       [:body
        [:div.container
         [:div.unseen-count [:p (case (count unseen)
                                  0 "no new items"
                                  1 "1 new item"
                                  (str (count unseen) " new items"))]]
         (map #(render-item :html %) unseen)
         (when (seq unseen)
           [:div.separator])
         (map #(render-item :html %) seen)]]])))

(def handler
  (site
    (routes
      (GET "/" []
        (index&)))))

(defn handler-wrapper [request]
  (let [handler (-> handler
                    (wrap-resource "public"))]
    (handler request)))

(defn run-server! []
  (run-jetty handler-wrapper {:host "0.0.0.0" :port 3000 :join? false}))

#_ (run-server!)
