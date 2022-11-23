(ns feedn.source.interface)

(defmulti fetch-items
  "Fetch items from source and return a sequence of items"
  (fn [source channel sub-config]
    source))

(defmulti render-item-body
  "Render the body of an item for display in the given format. Dispatches on format and item's source"
  (fn [fmt item]
    [fmt (:source item)]))

(defmulti render-item
  "Render item for display in the given format. Dispatches on format"
  (fn [fmt item]
    fmt))
