(ns way.vega
  (:require
   [reagent.core :as reagent]
   ["react-vega" :as rv]))

(def vega-lite-adapter (reagent/adapt-react-class rv/VegaLite))

(defn vega-lite-view
  [spec data]
  (when data
    [vega-lite-adapter {:data (clj->js data) ;I'm not sure this works?
                        :spec (clj->js spec)
                        :actions false}])) ;TODO on in dev mode

(def vega-adapter (reagent/adapt-react-class rv/Vega))

(defn vega-view
  [spec data]
  (when data
    [vega-adapter {:data (clj->js data)
                   :spec (clj->js spec)
                   :actions false
                   }]))



