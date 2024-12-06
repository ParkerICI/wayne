(ns wayne.frontend.examples
  (:require
   [re-frame.core :as rf]
   [org.candelbio.multitool.core :as u]
   [com.hyperphor.way.web-utils :as wu]
   ))

;;; TODO vis parameters 

(def examples
  [{:text "Find relative intensity of fucosylated glycans in WHO grade 2,3 and 4 samples. "
    :params {:universal {:dim :WHO_grade, :feature "fucosylated"},
             :heatmap2 {:dim :WHO_grade},
             :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Glycan", :feature-feature_type "Relative_Intensity", :feature-feature_variable "fucosylated"}
             :violin {"blobWidth" 100 "blobSpace" 328}
             }
    }
   {:text "Find the proportion of tumor cells that have high intensity B7H3 in GBM, Astrocytoma and Oligodendroglioma samples."
    :params {:universal {:dim :Tumor_Diagnosis, :feature "B7H3_segment_high_prop", :filters {:Tumor_Diagnosis {"Astrocytoma" true, "GBM" true, "Oligodendroglioma" true}}},
             :heatmap2 {:dim :Tumor_Diagnosis, :filter {:Tumor_Diagnosis {"Astrocytoma" true, "GBM" true, "Oligodendroglioma" true}}},
             :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Tumor_Antigens_Intensity_Segments", :feature-feature_variable "B7H3_segment_high_prop"}
             :violin {"blobWidth" 100 "blobSpace" 328}
             }
    }
   {:text "Find mean intensity of EGFR expression on tumor cells in primary WHO grade 3 and 4 samples."
    :params {:universal {:dim :WHO_grade,
                         :feature "EGFR",
                         :filters {:recurrence {"No" true},
                                   :WHO_grade {"3" true, "4" true}}},
             :heatmap2 {:dim :WHO_grade, :filter {:Tumor_Diagnosis {"Astrocytoma" false, "GBM" false, "Oligodendroglioma" false}, :recurrence {"No" true}, :WHO_grade {"3" true, "4" true}}},
             :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Tumor_Antigens_Intensity", :feature-feature_variable "EGFR"}
             :violin {"blobWidth" 100 "blobSpace" 328}}
    
    }
   {:text "Find mean intensity of TOX expression in recurrent GBM samples and compare between control and Neoadjuvant PD1 Trial 1"
    :params {:universal {:dim :treatment,
                         :feature "Tox",
                         :filters {:Tumor_Diagnosis {"GBM" true},
                                   :treatment {"Neoadjuvant_PD1_Trial_1" true, "Treatment_Naive" true},
                                   :recurrence {"Yes" true}}},
             :heatmap2 {:dim :treatment, :filter {:Tumor_Diagnosis {"Astrocytoma" false, "GBM" true, "Oligodendroglioma" false}, :recurrence {"No" true, "Yes" true}, :WHO_grade {"3" true, "4" true}, :Immunotherapy {"true" false}, :treatment {"Neoadjuvant_PD1_Trial_1" true, "Treatment_Naive" true}}}, :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Functional_marker_intensity", :feature-feature_variable "Tox"}, :heatmap {:filter {}}},
    
    }
   {:text "Find mean intensity of CD14 expression in primary WHO grade 4 samples and compare between IDH mutant and wild type"
    :params {:universal {:dim :IDH_R132H_Status,
                         :feature "CD14",
                         :filters {:WHO_grade {"4" true},
                                   :recurrence {"No" true}}},
             :heatmap2 {:dim :IDH_R132H_Status, :filter {:Tumor_Diagnosis {"Astrocytoma" false, "GBM" true, "Oligodendroglioma" false}, :recurrence {"No" true, "Yes" true}, :WHO_grade {"3" true, "4" true}, :Immunotherapy {"true" false}, :treatment {"Neoadjuvant_PD1_Trial_1" true, "Treatment_Naive" true}}}, :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Phenotype_marker_intensity", :feature-feature_variable "CD14"}, :heatmap {:filter {}}}
    
    }

   ])


(rf/reg-event-db
 :remember-example
 (fn [db _]
   (let [example (select-keys db [:params             ;includes query and feature selector param
                                  ])]
     (prn :example example)
     (assoc db
            :example example))))

(rf/reg-event-db
 :recall-example
 (fn [db [_ text]]
   (let [example (u/some-thing #(= (:text %) text) examples)]
     ;; TODO no this is wrong, sorry
     #_ (u/merge-recursive db example)
     ;; Maybe be smart?
     (rf/dispatch [:open-collapse-panel :dim])
     (rf/dispatch [:open-collapse-panel :feature])
     (rf/dispatch [:open-collapse-panel :viz])
     ;; Open one filter pane, best we can do now
     (when-let [filter-dim (first (keys (get-in example [:params :universal :filters])))]

       (rf/dispatch [:open-filter-pane filter-dim]))

     (assoc db :params (:params example))
     )))

(defn example-chooser
  []
  (wu/select-widget
   :example nil #(rf/dispatch [:recall-example %])
   (map :text examples)
   "Choose an example"))
