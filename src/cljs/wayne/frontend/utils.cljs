(ns wayne.frontend.utils
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
  [img]
  (let [id (gensym)]
  [:span 
   [:img.info
    {:src "../assets/icons/help-icon-16997.png"
     :on-mouse-over #(show-image id true)
     :on-mouse-out #(show-image id false)
     }]
   [:img.img-popup.collapsed {:src img :id id :height 500}]]))
