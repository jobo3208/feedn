(ns feedn.frontend
  (:require [compojure.core :refer [routes GET]]
            [compojure.handler :refer [site]]
            [feedn.source :refer [render-item]]
            [feedn.timeline :refer [get-timeline]]
            [hiccup.core :refer [html]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as resp]))

(defn index& []
  (html
    [:html
     [:head
      [:link {:rel "stylesheet" :href "style.css"}]]
     [:body
      [:div.container
       (map #(render-item :html %) (get-timeline))]]]))

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
