(ns way.vega
  (:require
   [reagent.core :as reagent]
   ["react-vega" :as rv]))

(def vega-lite-adapter (reagent/adapt-react-class rv/VegaLite))

(defn vega-lite-view
  [spec data]
  (when data
    [vega-lite-adapter {:data (clj->js data) :spec (clj->js spec)}]))


;;; This is a very non-react way to do things. TODO make ir more react to make hot-reloading easier

;;; TODO actual BUG that might get fixed by this. Go to sites pane and see the vega view, navigate away and then back, and the vega has disappeared. 


;;; One way
;;; https://github.com/Day8/re-frame/blob/master/docs/Using-Stateful-JS-Components.md
;;; But probably better way 



;;; Spec is vega spec with data
;;; dom-id is a dom specifier like "#viz"
(defn do-vega
  [spec dom-id]
  (js/module$node_modules$vega_embed$build$vega_embed.embed dom-id (clj->js spec)))


