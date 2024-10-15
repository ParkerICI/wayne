(ns wayne.frontend.autocomplete
  (:require [re-frame.core :as rf]
            #_ [com.hyperphor.way.api :as api]
            [com.hyperphor.way.feeds :as feeds]
            [clojure.string :as str]
            ))

;;; → Way (but needs to be generalized)
;;; Loosely based on Alzabo's autocomplete

(rf/reg-event-db
 :user-string-change
 (fn [db [_ user-string]]
   ;; Using [:data] machinery, save some work (TODO make sure it isn't retaining too much)
   #_ (api/api-get "/ac" {:query-params {:user user-string}
                       :on-success (fn [response]
                                     (rf/dispatch [:choices-change response]))})
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
   (-> db
       (assoc :user-string choice)
       (assoc-in [:data :rna-autocomplete] []) ;clear the popup early, better UX
       (assoc-in [:params :features :feature-feature_variable] choice) ;integrate with search
       )))

(defn render-item
  [choice user-string]
  [:div {:on-click #(rf/dispatch [:choose choice])} choice]) ;TODO highlight

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
                            [:user-string-change (-> e .-target .-value)]))}]

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

