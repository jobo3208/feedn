(ns feedn.source.dumpor
  (:require [clojure.string :as string]
            [feedn.source :refer [fetch-items render-item-body]]
            [feedn.util :refer [select-text]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [net.cgrand.enlive-html :as xml]))

(defn- parse-ago-str
  ([s]
   (parse-ago-str s (jt/instant)))
  ([s from]
   (let [[amount unit ago] (string/split s #" ")
         _ (assert (= ago "ago"))
         amount (Integer. amount)
         unit (if (= (last unit) \s)
                (subs unit 0 (dec (count unit)))
                unit)
         _ (assert (#{"second" "minute" "hour" "day" "week" "month" "year"} unit))
         ; convert to weeks if necessary, since java-time does not support
         ; arithmetic with months or years
         [amount unit] (case unit
                         "month" [(* 4 amount) "week"]
                         "year" [(* 52 amount) "week"]
                         [amount unit])
         duration-fn (resolve (symbol "java-time" (str unit "s")))]
     (jt/minus from (duration-fn amount)))))

(defn- parse-item [item]
  (let [content (xml/select item [:.content__img-wrap])
        rel-link (-> content (xml/select [xml/root :> :a]) first :attrs :href)
        link (str "https://dumpor.com" rel-link)
        guid (str "dumpor:" (last (string/split rel-link #"/")))]
    {:content (-> content
                  (xml/at
                    [xml/root :> :a] (xml/set-attr :href link)
                    [:.content__btns] nil)
                  (xml/emit*)
                  (as-> s (apply str s)))
     :pub-date (parse-ago-str (select-text item [(xml/has [:> :.bx-time]) :span]))
     :guid guid
     :link link}))

(defn- parse [source channel opts doc]
  (let [account-name (select-text doc [:.user :h1])
        account-handle (select-text doc [:.user :h4])
        items (->> (xml/select doc [[:.card (xml/but :.ads)]])
                   (map #(parse-item %))
                   (map #(assoc %
                                :source source
                                :channel channel
                                :dumpor/account-name account-name
                                :dumpor/account-handle account-handle)))]
     items))

(defmethod fetch-items :dumpor
  ([source channel]
   (fetch-items source channel {}))
  ([source channel opts]
   (let [url (java.net.URL. (str "https://dumpor.com/v/" channel))
         doc (try
               (xml/html-resource url)
               (catch Exception e
                 (throw (ex-info "fetch error" {:type :fetch :url url} e))))
         items (try
                 (parse source channel opts doc)
                 (catch Exception e
                   (throw (ex-info "parse error" {:type :parse} e))))]
     items)))

(defmethod render-item-body [:html :dumpor]
  [_ item]
  (html
    [:div.item-body
     [:h3 {:class "card-title" :id (:guid item)} (:dumpor/account-name item) " (" (:dumpor/account-handle item) ")"]
     (:content item)]))
