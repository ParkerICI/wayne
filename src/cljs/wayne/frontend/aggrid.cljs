(ns wayne.frontend.aggrid
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            ["ag-grid-community" :as ag]
            ["ag-grid-enterprise" :as age]
            ["ag-grid-react" :as agr]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.web-utils :as wu]
            [wayne.ops-def :as ops]
            [reagent.dom]
            [clojure.string :as str]
            )
  #_ (:require-macros
      [wayne.macros :refer (ag-grid-license)]))

;;; Wrapping of ag-grid. 

;;; For header interactivty, see https://ag-grid.com/react-data-grid/component-header/

(def ag-adapter (reagent/adapt-react-class agr/AgGridReact))

(comment
  (def license-key (ag-grid-license))

;;; Supposed to be done once, thus this top-level call.
(when license-key
  (.setLicenseKey age/LicenseManager license-key)))


;;; TODO next realer design: have coldefs that include display type, smarter dispatch...
(defmulti ag-col-def (fn [col-spec op]
                        (get op :display)))
                          
(defn vecify
  [v]
  (if (vector? v)
    v
    (vector v)))

(def header-template
  "<div class=\"ag-cell-label-container\" role=\"presentation\" style='background:lightskyblue;'>
<span ref=\"eMenu\" class=\"ag-header-icon ag-header-cell-menu-button\"></span>
<div ref=\"eLabel\" class=\"ag-header-cell-label\" role=\"presentation\">
<span ref=\"eSortOrder\" class=\"ag-header-icon ag-sort-order\" ></span>
    <span ref=\"eSortAsc\" class=\"ag-header-icon ag-sort-ascending-icon\" ></span>
    <span ref=\"eSortDesc\" class=\"ag-header-icon ag-sort-descending-icon\" ></span>
    <span ref=\"eSortNone\" class=\"ag-header-icon ag-sort-none-icon\" ></span>
    <span ref=\"eText\" class=\"ag-header-cell-text\" role=\"columnheader\"></span>
    <span ref=\"eFilter\" class=\"ag-header-icon ag-filter-icon\"></span>
 </div>
  </div>")


(defmethod ag-col-def :default [col-def op]
  (let [url-template (:url-template op)]
    {:headerName (:label col-def)
     :field (:id col-def)
     :cellRenderer (fn [params]
                     (let [raw (get (js->clj (.-data params) :keywordize-keys true) (:id col-def))
                           value (vecify raw)
                           link (when url-template (str url-template raw)) ; TODO need better templateing, this works for now
                           render (if link
                                    (fn [v] [:a.ent {:href link :target "_ext"} (js/decodeURIComponent (str v))])
                                    (fn [v] [:span (str v)]))]
                       (reagent.dom/render ;TODO this is not approved for React 18, but I couldn't figure a better way.
                         [:span.ag-cell-wrap-text   ;; .ag-cell-auto-height doesn't work, unfortunately.
                          (when (and (vector? raw) (> (count raw) 1))
                            [:span.count (count raw)])
                          (for [elt (butlast value)]
                            [:span (render elt) ", "])
                          (render (last value))]
                         (.-eGridCell params))))
     ;; TODO really want to highlight the whole column, this is OK for now I guess
     :headerComponentParams
     {:template (when (= (name (:id col-def)) @(rf/subscribe [:col-select]))
                  header-template)
      }}))

(defmethod ag-col-def :sparkline [col op]
  {:headerName (:label col)
   :field (:id col) 
   :cellRenderer "agSparklineCellRenderer"
   :cellRendererParams
   {:sparklineOptions
    {:type "line"
     :line
     {:stroke "rgb(0, 0, 178)"
      :strokeWidth 2}
     :marker
     {:size 3,
      :shape "diamond",
      :fill "green",
      :stroke "green",
      :strokeWidth 2
    },
     :xKey "period"
     :yKey "value"
     }
    }
   }
  )

(defmethod ag-col-def :image [col op]
  {:headerName (:label col)
   :field (:id col) 
   :cellRenderer (fn [params]
                   (let [url (get (js->clj (.-data params) :keywordize-keys true) (:id col))]
                     (reagent.dom/render
                       [:img {:src url :height 30}]   ;; .ag-cell-auto-height doesn't work, unfortunately.
                       (.-eGridCell params))))
   }
  )



(defn wrap-brackets
  [s]
  (str "[" s "]"))


;;; Keep pointers to API objects for various purposes. This maps a keyword id to the appropriate API object.
;;; Note: this doesn't survive a figwheel reload. Maybe store in the re-frame db instead?
(def ag-apis (atom {}))

(defn jsx->clj
  [x]
  (into {} (for [k (.keys js/Object x)] [(keyword k) (aget x k)])))

(rf/reg-event-db
 :col-select
 (fn [db [_ col]]
   (assoc db :column-selected col)))

(rf/reg-sub
 :col-select
 (fn [db _]
   (:column-selected db)))

(defn ag-col-def1
  [col-spec]
  (ag-col-def col-spec (get ops/ops-indexed (:op col-spec))))

(defn ag-table
  "id: a keyword to identify this table
  columns: seq of columns, which can be a keyword or a map (TODO)
     :editable? : boolean to control editability
     :formatter : a fn on values to produce their display form
  data: a seq of maps
  ag-grid-options: a map of values passed to ag-grid
  checkboxes?: control whether checkboxes appear, defaults true
  class: css class to use for grid"
  [id columns data ag-grid-options & {:keys [checkboxes? class] :or {checkboxes? true}}]
  (if (empty? columns)
    (wu/spinner)
    (let [column-defs (mapv ag-col-def1 columns)
          column-defs (if checkboxes?
                        (-> column-defs
                            (assoc-in [0 :checkboxSelection] true)
                            (assoc-in [0 :headerCheckboxSelection] true)
                            (assoc-in [0 :headerCheckboxSelectionFilteredOnly] true))
                        column-defs)
          ]
      [:div.ag-container {:class class}
       [:div {:className "ag-theme-balham"}
        (let [grid-options
              (merge                     ;merges grid options, optional detail grid options, and user supplied options
               {:defaultColDef {:sortable true
                                :filter "agTextColumnFilter" ; TODO have type-specific filters, see https://www.ag-grid.com/javascript-grid-filtering/#configuring-filters-to-columns
                                :resizable true
                                :minWidth 55 ;TODO(ag-grid) autoSize doesn't seem to respect this, it should
                                }
                :onGridReady (fn [params]
                               (swap! ag-apis assoc id (.-api params)))
                :onFirstDataRendered (fn [params]
                                       (let [column-api (.-columnApi params)] 
                                         (.autoSizeColumns column-api (apply array (map :field column-defs)))))
                :columnDefs column-defs
                ;; :suppressFieldDotNotation true
                :rowData data
                :suppressRowHoverHighlight true ;Be column-centric
                :columnHoverHighlight true
                ;; :rowSelection "multiple"  ; no, this is fugly, use checkboxes if we need to do this
                ;; :rowMultiSelectWithClick true
                ;; :rowDeselection true
                :onColumnHeaderClicked (fn [params]
                                         (let [col (jsx->clj (.-column params))]
                                           (rf/dispatch [:col-select (:colId col)])))
                :pagination true
                :paginationAutoPageSize true
                :sideBar {:hiddenByDefault false ; visible but closed
                          :toolPanels [{:id "columns"
                                        :labelDefault "Columns"
                                        :labelKey "columns"
                                        :iconKey "columns"
                                        :toolPanel "agColumnsToolPanel"
                                        ;; Turning these off for now, might want to revisit in the future. Possibly incompatible with the master/detail feature?
                                        :toolPanelParams {:suppressRowGroups true
                                                          :suppressValues true
                                                          :suppressPivots true 
                                                          :suppressPivotMode true}
                                        }
                                       {:id "filters"
                                        :labelDefault "Filters"
                                        :labelKey "filters"
                                        :iconKey "filter"
                                        :toolPanel "agFiltersToolPanel"
                                        }]
                          }
                :animateRows true
                :statusBar {:statusPanels [{:statusPanel "agTotalAndFilteredRowCountComponent"
                                            :align "left"}]}
                }
               ag-grid-options)]
          ;; debug tool, for reporting config to ag-grid.com
          ;;         (print (.stringify js/JSON (clj->js grid-options)))
          [ag-adapter grid-options])
        ]])))


;;; See https://ag-grid.com/react-data-grid/view-refresh/#redraw-rows
;;; This could be smarter and just redraw specific rows, but I'm lazy.
(defn redraw
  [gr-id]
  (.redrawRows (get @ag-apis gr-id)))
