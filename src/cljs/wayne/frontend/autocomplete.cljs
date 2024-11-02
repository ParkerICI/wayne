(ns wayne.frontend.autocomplete
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [org.candelbio.multitool.core :as u]
            ))

;;; → Way (but needs to be generalized)
;;; Loosely based on Alzabo's autocomplete

(rf/reg-event-db
 :user-string-change
 (fn [db [_ user-string]]
   ;; Using [:data] machinery, save some work
   (-> db
       (assoc :user-string user-string)
       ;; Shouldn't be necessary, should be automated in subscription TODO
       (assoc-in [:data-status :rna-autocomplete] :invalid)
       )))

(rf/reg-sub
  :user-string
  (fn [db _]  
    (:user-string db "")))

(rf/reg-event-db
 :choose
 (fn [db [_ choice]]
   (if (some #(= % choice) (get-in db [:data :rna-autocomplete]))
     (-> db
         (assoc :user-string choice)
         (assoc-in [:data :rna-autocomplete] []) ;clear the popup early, better UX
         (assoc-in [:params :features :feature-feature_variable] choice) ;integrate with search
         )
     ;; not a legit choice, probably came from a blur, clear completion pane and  ignore 
     (-> db
         (assoc-in [:data :rna-autocomplete] [])))))

(defn render-item
  [choice user-string]                  ;TODO user-string highlight
  [:div {:on-click #(rf/dispatch [:choose choice])} choice]) 

(defn ui
  []
  (let [user-string @(rf/subscribe [:user-string])
        prefix (str/upper-case user-string)
        choices (when (> (count user-string) 1)
                  @(rf/subscribe [:data :rna-autocomplete {:prefix prefix}]))
        ]
    [:div 
     [:input {:value user-string
              :on-change (fn [e]
                           (rf/dispatch
                            [:user-string-change (-> e .-target .-value)]))
              :on-blur  (fn [e]
                          (when (and (= 1 (count choices)) 
                                     (= (first choices) (-> e .-target .-value)))
                            (rf/dispatch [:choose (first choices)])))
              }]

     (when-not (or (empty? choices)
                   ;; TODO not quite right in cases where one entry is a prefix of another (eg PODXL)
                   (and (= 1 (count choices)) ; don't show if we've just clicked an item
                        (= (first choices) user-string)))
       [:div.popup
        [:div.popuptext {:style {:visibility (if (empty? choices) "hidden" "visible")}}
         [:table                        ;TODO doesn't really need to be a table
          [:tbody
           (for [choice choices]
             ^{:key choice}
             [:tr
              [:td (render-item choice user-string)]])]]]])]
    ))

