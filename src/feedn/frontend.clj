(ns feedn.frontend
  (:require [clojure.java.io :as io]
            [compojure.core :refer [routes GET POST]]
            [compojure.handler :refer [site]]
            [feedn.limit :as limit]
            [feedn.source :refer [render-item]]
            [feedn.state :refer [state*]]
            [feedn.timeline :refer [get-timeline mark-seen!]]
            [hiccup.core :refer [html]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :as resp]))

(def FILTER-PARAMS [:source :channel :tag])

(defmulti coerce-filter-param (fn [k v] k))
(defmethod coerce-filter-param :source [_ v] (keyword v))
(defmethod coerce-filter-param :tag [_ v] (keyword v))
(defmethod coerce-filter-param :default [_ v] v)

(defn coerce-filter-params [filter-params]
  (->> filter-params
       (map (fn [[k v]]
              [k (coerce-filter-param k v)]))
       (into {})))

(defmulti param->filter-fn (fn [k v] k))
(defmethod param->filter-fn :tag [_ v] (fn [item] ((:tags item) v)))
(defmethod param->filter-fn :default [k v] (comp (partial = v) k))

(defn params->filter-fn [params]
  (apply every-pred (map #(apply param->filter-fn %) params)))

(def clear-filters-emoji "\uD83D\uDEAB")

(defn base-tpl [body]
  (html
    [:html
     [:head
      [:meta {:name :viewport :content "width=device-width, initial-scale=1.0"}]
      [:style (slurp (io/resource "public/style.css"))]]
     [:body body]]))

(defn index-view [req]
  (let [state @state*
        params (:params req)
        filter-params (select-keys params FILTER-PARAMS)
        filter-params (coerce-filter-params filter-params)
        filter-fn (if (seq filter-params)
                    (params->filter-fn filter-params)
                    (constantly true))
        timeline (->> (get-timeline)
                      (filter filter-fn)
                      (filter #(>= (:volume state) (:min-volume %))))
        [after-cutoff before-cutoff] (split-with (partial limit/after-cutoff? state) timeline)
        [unseen seen] (split-with (comp not :seen?) before-cutoff)]
    (mark-seen! unseen)
    (base-tpl
      (html
        [:div.container
         [:div.page-header
          [:div
           [:form.post-link {:action "/update" :method :post}
            [:button {:type :submit} "update"]
            (str " (" (:limit/updates-remaining state) ")")]]
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
  (base-tpl
    (html
      [:div.container
       [:div.page-header "settings"]
       [:form {:method :post}
        [:dl
         [:dt "volume"]
         [:dd
          [:input {:type :range :min 1 :max 3 :step 1 :name "volume" :value (:volume @state*)}]]]
        [:button {:type :submit} "save"]]])))

(def handler
  (site
    (routes
      (GET "/" [_ :as req]
        (index-view req))
      (POST "/update" []
        (swap! state* limit/register-update)
        (resp/redirect "/"))
      (GET "/settings" []
        (settings-view))
      (POST "/settings" [_ :as req]
        (let [volume (-> req :params :volume (Integer.))]
          (assert (#{1 2 3} volume))
          (swap! state* assoc :volume volume)
          (resp/redirect "/"))))))

(defn handler-wrapper [request]
  (handler request))

(defn run-server! []
  (run-jetty handler-wrapper {:host "0.0.0.0" :port 3000 :join? false}))

#_ (run-server!)
