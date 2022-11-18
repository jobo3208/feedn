(ns feedn.frontend
  (:require [clojure.java.io :as io]
            [compojure.core :refer [routes GET POST]]
            [compojure.handler :refer [site]]
            [feedn.config :refer [config_]]
            [feedn.frontend.filter :refer [params->filter-params filter-params->fn]]
            [feedn.limit :as limit]
            [feedn.source :refer [render-item]]
            [feedn.state :refer [state_]]
            [feedn.timeline :refer [get-timeline mark-seen]]
            [hiccup.core :refer [html]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as resp]))

(defn base-template [body]
  (html
    [:html
     [:head
      [:meta {:name :viewport :content "width=device-width, initial-scale=1.0"}]
      [:link {:rel "apple-touch-icon" :sizes "180x180" :href "/apple-touch-icon.png"}]
      [:link {:rel "icon" :type "image/png" :sizes "32x32" :href "/favicon-32x32.png"}]
      [:link {:rel "icon" :type "image/png" :sizes "16x16" :href "/favicon-16x16.png"}]
      [:link {:rel "manifest" :href "/site.webmanifest"}]
      [:style (slurp (io/resource "public/style.css"))]
      [:title "feedn"]]
     [:body body]]))

(def clear-filters-emoji "\uD83D\uDEAB")

(defn index-view [req]
  (let [state @state_
        config @config_
        filter-params (params->filter-params (:params req))
        filter-fn (if (seq filter-params)
                    (filter-params->fn filter-params)
                    (constantly true))
        timeline (->> (get-timeline state config)
                      (filter filter-fn)
                      (filter #(>= (:volume state) (:min-volume %))))
        timeline (drop-while (partial limit/after-cutoff? state) timeline)
        [unseen seen] (split-with (comp not :seen?) timeline)]
    (swap! state_ mark-seen (map :guid unseen))
    (base-template
      (html
        [:div.container
         [:div.page-header
          [:div
           [:form.post-link {:action "/update" :method :post}
            [:button {:type :submit} "update"]
            (str " (" (:updates-remaining state) ")")]]
          [:div {:style "text-align: center;"}
           (if (seq unseen)
             [:a {:href (str "#" (-> unseen last :guid))}
              (if (= 1 (count unseen))
                "1 new item"
                (str (count unseen) " new items"))]
             "no new items")
           (when (seq filter-params)
            [:span " " [:a {:href "/" :class :emoji-link :title "clear filters"} clear-filters-emoji]])]
          [:div {:style "text-align: right;"}
           [:a {:href "/settings"} "settings"]]]
         (map #(render-item :html %) unseen)
         (when (seq unseen)
           [:div.separator])
         (map #(render-item :html %) seen)]))))

(defn settings-view []
  (base-template
    (html
      [:div.container
       [:div.page-header "settings"]
       [:form {:method :post}
        [:dl
         [:dt "volume"]
         [:dd
          [:input {:type :range :min 0 :max 3 :step 1 :name "volume" :value (:volume @state_)}]]]
        [:button {:type :submit} "save"]]])))

(def handler
  (site
    (routes
      (GET "/" [_ :as req]
        (index-view req))
      (POST "/update" []
        (swap! state_ limit/register-update @config_)
        (resp/redirect "/"))
      (GET "/settings" []
        (settings-view))
      (POST "/settings" [_ :as req]
        (let [volume (-> req :params :volume (Integer.))]
          (assert (#{0 1 2 3} volume))
          (swap! state_ assoc :volume volume)
          (resp/redirect "/"))))))

(defn handler-wrapper [request]
  (let [handler (-> handler
                    (wrap-resource "public"))]
    (handler request)))

(defn run-server! []
  (run-jetty handler-wrapper {:host "0.0.0.0" :port 3000 :join? false}))
