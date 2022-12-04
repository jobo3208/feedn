(ns feedn.source.interface)

(defmulti fetch-items
  "Fetch items from source and return a sequence of items"
  (fn [source channel sub-config]
    source))

(defmulti render-item
  "Render item for display in the given format. Dispatches on format"
  (fn [fmt item]
    fmt))
