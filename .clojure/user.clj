(in-ns 'user)
(require '[clj-reload.core :as reload])


(defn reload-namespaces [] (reload/reload))
