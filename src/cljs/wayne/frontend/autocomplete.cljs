(ns wayne.frontend.autocomplete
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.api :as api]
            ))

;;; → Way
;;; Loosely based on Alzabo's autocomplete

(rf/reg-event-db              ;; sets up initial application state
 :initialize                 ;; usage:  (dispatch [:initialize])
 (fn [_ _]                   ;; the two parameters are not important here, so use _
     {:user-string ""
      :choices []
      }))

(rf/reg-event-db
 :user-string-change
 (fn [db [_ user-string]]
   (api/api-get "/ac" {:query-params {:user user-string}
                       :on-success (fn [response]
                                     (rf/dispatch [:choices-change response]))})
   (assoc db :user-string user-string)))

(rf/reg-sub
  :user-string
  (fn [db _]  
    (:user-string db "")))

(rf/reg-sub
  :choices
  (fn [db _]  
    (:choices db)))

(defn render-item
  [choice user-string]
  [:div choice])                        ;TODO highlight

(defn ui
  []
  (let [user-string @(rf/subscribe [:user-string])
        choices @(rf/subscribe [:choices]) ]
    [:div {:style {:padding "10px"}}
     [:div
      "Gene: "
      [:input {:value user-string
               :on-change (fn [e]
                            (rf/dispatch
                             [:user-string-change (-> e .-target .-value)]))}]

      ]
     [:div.popup
      [:div.popuptext {:style {:visibility (if (empty? choices) "hidden" "visible")}}
       (if (empty? choices)
         [:div [:h3 "No results"]]
         [:table
          [:tbody
           ;; TODO sorting
           (for [choice choices]
             ^{:key choice}
             [:tr
              [:td (render-item choice user-string)]])]])]]]
    ))

