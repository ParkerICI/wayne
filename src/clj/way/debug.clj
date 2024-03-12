(ns way.debug
  (:require [seesaw.core :as ss]
            [org.candelbio.multitool.core :as u ]
            [clojure.pprint :as pp]))

(defn pprint-str
  [thing]
  (with-out-str
    (pp/pprint thing)))

(u/defn-memoized window
  [id]
  (-> (ss/frame :title (str id)
                :visible? true
                )
      ss/pack!
      #_ ss/show!)
  )

(defn set-content
  [w thing]
  (.setContentPane w
                   (ss/text
                    :text (pprint-str thing)
                    :multi-line? true 
                    ;; :wrap-lines? true 
                    :editable? false 
                    ;;:font normal-font
                    ))
  (.pack w))

(defn view
  [id thing]
  (set-content (window id) thing))

