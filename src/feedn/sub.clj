(ns feedn.sub)

(defn get-sub-state [state source channel]
  (get-in state [:subs :sources source :channels channel]))

(defn get-sub-config [config source channel]
  (merge
    (-> config :subs (dissoc :sources))
    (-> config (get-in [:subs :sources source]) (dissoc :channels))
    (-> config (get-in [:subs :sources source :channels channel]))))

(defn iter-subs
  "Return a lazy sequence of [source channel sub-map] from state-or-config's subs"
  [state-or-config]
  (for [[sk sv] (:sources (:subs state-or-config))
        [ck cv] (:channels sv)]
    [sk ck cv]))

(defn update-subs
  "Apply f to all subs in state-or-config, where f is a function of 3 args: source, channel, and sub-map"
  [state-or-config f]
  (assoc
    state-or-config
    :subs
    (reduce
      (fn [m [source channel sub-map]]
        (assoc-in m [:sources source :channels channel] (f source channel sub-map)))
      {}
      (iter-subs state-or-config))))
