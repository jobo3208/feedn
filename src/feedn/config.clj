(ns feedn.config
  (:require [clojure.spec.alpha :as s]
            [clojure.edn :as edn]
            [feedn.util :refer [deep-merge]]))

; primitives
(s/def ::tag keyword?)
(s/def ::emoji string?)

; global settings
(s/def ::volume nat-int?)  ; default volume level
(s/def ::updates-remaining nat-int?)  ; allowed number of daily updates

; per-channel settings. note that these can be set at the channel, source, or
; subs (top) level, and they will apply to all channels within.
(s/def ::period pos-int?)  ; time between updates (seconds)
(s/def ::max-items pos-int?)  ; keep max-items most recent items per channel
(s/def ::min-volume pos-int?)  ; lowest volume setting where items from this channel will be displayed
(s/def ::color string?)  ; background color of items in this channel
(s/def :feedn.config.channel/tags (s/coll-of ::tag :kind set?))  ; tags for this channel

; configuration structure
(s/def ::channel-name string?)
(s/def ::channel (s/keys :opt-un [::period ::max-items ::min-volume ::color :feedn.config.channel/tags]))
(s/def ::channels (s/map-of ::channel-name ::channel))
(s/def ::source keyword?)
(s/def ::sources (s/map-of ::source (s/keys :opt-un [::channels ::period ::max-items ::min-volume ::color :feedn.config.channel/tags])))
(s/def ::subs (s/keys :opt-un [::sources ::period ::max-items ::min-volume ::color :feedn.config.channel/tags]))
(s/def ::tags (s/map-of ::tag (s/keys :opt-un [::emoji])))
(s/def ::config (s/keys :opt-un [::subs ::tags ::updates-remaining ::volume]))

(def default-config
  {:subs
   {:period 120
    :max-items 10
    :min-volume 2
    :color "#ddd"}
   :updates-remaining 24
   :volume 2})

(defonce config_ (atom default-config))

(defn load-config [filepath]
  (let [config' (-> filepath slurp edn/read-string)]
    (if-let [error (s/explain-data ::config config')]
      (throw (ex-info "Invalid config" error))
      (deep-merge default-config config'))))

(defn load-config! [filepath]
  (reset! config_ (load-config filepath)))
