(ns way.aggrid
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            ["ag-grid-community" :as ag]
            ["ag-grid-enterprise" :as age]
            ["ag-grid-react" :as agr]
            [org.candelbio.multitool.core :as u]
            [way.web-utils :as wu]
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


;;; Keep pointers to API objects for various purposes. This maps a keyword id to the appropriate API object.
;;; Note: this doesn't survive a figwheel reload. Maybe store in the re-frame db instead?
(def ag-apis (atom {}))

(defn jsx->clj
  [x]
  (into {} (for [k (.keys js/Object x)] [(keyword k) (aget x k)])))

;;; To be complected I'm sure
(defn ag-col-def
  [col]
  {:headerName (name col)
   :field col
   }
  )

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
  (let [column-defs (mapv ag-col-def columns)          ]
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
              ;; :suppressRowHoverHighlight true ;Be column-centric
              ;; :columnHoverHighlight true
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
      ]]))


;;; See https://ag-grid.com/react-data-grid/view-refresh/#redraw-rows
;;; This could be smarter and just redraw specific rows, but I'm lazy.
(defn redraw
  [gr-id]
  (.redrawRows (get @ag-apis gr-id)))
