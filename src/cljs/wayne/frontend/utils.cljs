(ns wayne.frontend.utils
  )

(defn info
  [text]
  [:span {:data-tooltip text}
   [:img.info
    {:src "../assets/icons/help-icon-16997.png"
     :height 20
     :style {:cursor "pointer"      ;TODO move to .css
             :vertical-align "middle"
             :margin-left "7px"
             :margin-bottom "3px"}
     }]])
