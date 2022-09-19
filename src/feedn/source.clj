(ns feedn.source)

(defmulti fetch-items
  "Fetch items from source and return a sequence of items"
  (fn [source channel & opts]
    source))

(defmulti render-item
  "Render an item for display in the given format. Dispatches on format and item's source"
  (fn [fmt item & opts]
    [fmt (:source item)]))

(require 'feedn.source.nitter)
(require 'feedn.source.rotoworld)
(require 'feedn.source.invidious)
