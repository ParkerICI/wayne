(ns wayne.frontend.utils
  (:require [org.candelbio.multitool.core :as u])
  )

(defn open-popout
  [url & {:keys [width height id] :as args :or {id "_blank"}}]
  ;; Note: all the =no stuff doesn't work due to modern browser security features, oh well
  ;; TODO sizes seem to work radically differntly on Brave vs Safari. Not sure what's up with that.
  (.open js/window
         url
         id                       ; default _blank to make a new window each time
         (u/expand-template "resizable=no,toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=no,height={{height}},width={{width}}"
                            (merge {:width 500 :height 500} args))))

(defn popout-button
  [url & {:keys [label] :as args :or {label "Popout"}}]
  [:button {:type "button" :title label
            :on-click #(open-popout url args)}
   label]
  )

(defn info
  [text]
  [:span {:data-tooltip text}
   [:img.info
    {:src "../assets/icons/help-icon-16997.png"
     }]])

(defn show-image
  [id show?]
  (let [elt (.getElementById js/document id)]  
    (if show?
      (.remove (.-classList elt) "collapsed")
      (.add (.-classList elt) "collapsed"))))

(defn img-info
  [img width height]
  (let [id (gensym)]
    ;; Id is used bogth for popup image and popout window...just to be confusing. Should maybe use a more stable name for window to avoid popout proliferation problems
    [:span 
     [:img.info
      {:src "../assets/icons/help-icon-16997.png"
       :on-click #(open-popout img :width width :height height :id id)       ;TODO  title
       :on-mouse-over #(show-image id true)
       :on-mouse-out #(show-image id false)
       }]
     [:img.img-popup.collapsed {:src img :id id :height height}]]))
