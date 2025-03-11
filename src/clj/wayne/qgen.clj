(ns wayne.qgen
  (:require [org.candelbio.multitool.core :as u]
            [clj-http.client :as client]
            [environ.core :as env]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [org.candelbio.multitool.nlp :as nlp]
            [wayne.data-defs :as dd]
            [taoensso.timbre :as log]
            ))

;;; TODO should go through [wkok.openai-clojure.api :as api] for consistency although afaict that does very little

(defn query
  [q]
  (client/post "https://api.deepseek.com/chat/completions"
              {:as :json
               :headers {"Authorization" (str "Bearer " (env/env :deepseek-api-key))} 
               :content-type :application/json
               :body (json/write-str q)}))

(defn json-query
  [q]
  (-> q
      query
      (get-in [:body :choices 0 :message :content])
      #_ (assert "Failed to get response") ;TODO this happens a lot
      (json/read-str :key-fn keyword)))

(comment

  (query
   {:model "deepseek-chat"
    :messages [{:role :system :content "you are a helpful assistant"}
               {:role :user :content "What should I eat for dinner?"}]
    :stream false})

  (json-query
   {:model "deepseek-chat"
    :messages [{:role :system :content "you are a computational cancer biolologist. "}
               {:role :user :content "Please tell me about some drugs to treay hypertension. Please output in json format, including targets, mechanism of action, etc"}
               ]
    :response_format {:type "json_object"}
    :stream false}
   )
  )

;;; Kludgy fixes, but whaddya gonna do?
;;; TODO should constrain :dim to known values, and maybe same for others. Tends to get case wrong etc. 

(defn fixup-filters
  [fs]
  (u/map-values (fn [fsv] (u/map-keys name fsv)) fs))

;;; → Multitool contains, but does the sane thing for vectors
(defn vcontains?
  [v elt]
  (u/position= elt v))

;;; This is really inefficient, won't be suitable for bigger problems
(u/defn-memoized best-fit
  [key elts]
  (if (vcontains? elts key)
    key
    (u/min-by #(nlp/levenshtein (str/lower-case (name key)) (str/lower-case (name %)) )
              elts)))

;;; A start, but needs more
(defn fixup-query
  [q]
  (-> q
      (update-in [:params :universal :dim] #(best-fit (keyword %) (keys dd/dims)))
      (update-in [:params :universal :filters] fixup-filters)))

;;; 4 examples, more might be better?

(defn example-query
  [text]
  (->
   (json-query
    {:model #_ "deepseek-reasoner" "deepseek-chat"
     :messages [{:role :system :content "you are a computational cancer biolologist. "}
                {:role :user :content (str "Please translate this query to JSON, compute the params given the text: " text)}
                {:role :user :content "Sample JSON queries [{\"text\":\"Find relative intensity of fucosylated glycans in WHO grade 2,3 and 4 samples. \",\"params\":{\"universal\":{\"dim\":\"WHO_grade\",\"feature\":\"fucosylated\"},\"features\":{\"feature-bio-feature-type\":null,\"feature-supertype\":\"nonspatial\",\"scale\":\"linear\",\"feature-broad_feature_type\":\"Glycan\",\"feature-feature_type\":\"Relative_Intensity\",\"feature-feature_variable\":\"fucosylated\"},\"violin\":{\"blobWidth\":100,\"blobSpace\":328}}},
{\"text\":\"Find mean intensity of EGFR expression on tumor cells in primary WHO grade 3 and 4 samples.\",\"params\":{\"universal\":{\"dim\":\"WHO_grade\",\"feature\":\"EGFR\",\"filters\":{\"recurrence\":{\"No\":true},\"WHO_grade\":{\"3\":true,\"4\":true}}},\"features\":{\"feature-bio-feature-type\":null,\"feature-supertype\":\"nonspatial\",\"scale\":\"linear\",\"feature-broad_feature_type\":\"Protein\",\"feature-feature_type\":\"Tumor_Antigens_Intensity\",\"feature-feature_variable\":\"EGFR\"},\"violin\":{\"blobWidth\":100,\"blobSpace\":328}}},
{\"text\":\"Find mean intensity of TOX expression in recurrent GBM samples and compare between control and Neoadjuvant PD1 Trial 1\",\"params\":{\"universal\":{\"dim\":\"treatment\",\"feature\":\"Tox\",\"filters\":{\"Tumor_Diagnosis\":{\"GBM\":true},\"treatment\":{\"Neoadjuvant_PD1_Trial_1\":true,\"Treatment_Naive\":true},\"recurrence\":{\"Yes\":true}}},\"features\":{\"feature-bio-feature-type\":null,\"feature-supertype\":\"nonspatial\",\"scale\":\"linear\",\"feature-broad_feature_type\":\"Protein\",\"feature-feature_type\":\"Functional_marker_intensity\",\"feature-feature_variable\":\"Tox\"},\"heatmap\":{\"filter\":{}}}}]"}
                ]
     :response_format {:type "json_object"}
     :stream false})
   Fixup-query
   ))

#_
(example-query
 "Find the proportion of tumor cells that have high intensity B7H3 in GBM, Astrocytoma and Oligodendroglioma samples."
 )

(def example-response
  {:text "Blah"
   :params
   {:universal {:dim :Sex, :feature "VISTA", :filters {:WHO_grade {"3" true, "4" true}}},
    :features
    {:feature-bio-feature-type nil,
     :feature-supertype "nonspatial",
     :scale "linear",
     :feature-broad_feature_type "Protein",
     :feature-feature_type "Tumor_Antigens_Intensity",
     :feature-feature_variable "VISTA"},
    :violin {:blobWidth 100, :blobSpace 328}}})

(defn endpoint
  [query]
  (log/info "Query gen" query)
  (-> query
      example-query
  ))

(comment
;;; For NLP - trimmed most of the heatmap stuff which is not relevant
  (def examples-trimmed
    [
     {:text "Find relative intensity of fucosylated glycans in WHO grade 2,3 and 4 samples. "
      :params {:universal {:dim :WHO_grade, :feature "fucosylated"},
               :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Glycan", :feature-feature_type "Relative_Intensity", :feature-feature_variable "fucosylated"}
               :violin {"blobWidth" 100 "blobSpace" 328}
               }
      }
     ;; Holding off for testing
     #_ 
     {:text "Find the proportion of tumor cells that have high intensity B7H3 in GBM, Astrocytoma and Oligodendroglioma samples."
      :params {:universal {:dim :Tumor_Diagnosis, :feature "B7H3_segment_high_prop", :filters {:Tumor_Diagnosis {"Astrocytoma" true, "GBM" true, "Oligodendroglioma" true}}},
               :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Tumor_Antigens_Intensity_Segments", :feature-feature_variable "B7H3_segment_high_prop"}
               :violin {"blobWidth" 100 "blobSpace" 328}
               }
      }
     {:text "Find mean intensity of EGFR expression on tumor cells in primary WHO grade 3 and 4 samples."
      :params {:universal {:dim :WHO_grade,
                           :feature "EGFR",
                           :filters {:recurrence {"No" true},
                                     :WHO_grade {"3" true, "4" true}}},
               :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Tumor_Antigens_Intensity", :feature-feature_variable "EGFR"}
               :violin {"blobWidth" 100 "blobSpace" 328}}
      
      }
     {:text "Find mean intensity of TOX expression in recurrent GBM samples and compare between control and Neoadjuvant PD1 Trial 1"
      :params {:universal {:dim :treatment,
                           :feature "Tox",
                           :filters {:Tumor_Diagnosis {"GBM" true},
                                     :treatment {"Neoadjuvant_PD1_Trial_1" true, "Treatment_Naive" true},
                                     :recurrence {"Yes" true}}},
               :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Functional_marker_intensity", :feature-feature_variable "Tox"}, :heatmap {:filter {}}},
      
      }
     {:text "Find mean intensity of CD14 expression in primary WHO grade 4 samples and compare between IDH mutant and wild type"
      :params {:universal {:dim :IDH_R132H_Status,
                           :feature "CD14",
                           :filters {:WHO_grade {"4" true},
                                     :recurrence {"No" true}}},
               :features {:feature-bio-feature-type nil, :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Protein", :feature-feature_type "Phenotype_marker_intensity", :feature-feature_variable "CD14"}, :heatmap {:filter {}}}
      
      }
     {:text "Find the frequency of CD8 T cells around a 50-micron radius of Macrophage CD206 cells between WHO grade 4,GBM, Control and Neoadjuvant PD1 Trial 2 samples. "
      :params {:universal {:dim :treatment, :feature "Macrophage_CD206-Tcell_CD8", :filters {:WHO_grade {"4" true}, :treatment {"Neoadjuvant_PD1_Trial_2" true, "Treatment_Naive" true}, :Tumor_Diagnosis {"GBM" true}}}, :features {:feature-bio-feature-type nil, :feature-supertype "spatial", :scale "linear", :feature-broad_feature_type "Cells", :feature-feature_type "Neighborhood_Frequencies", :feature-feature_variable "EGFR", :subfeature-0 "Macrophage_CD206", :subfeature-2 "Tcell_CD8"}, :violin {"blobWidth" 100, "blobSpace" 277}}}

     {:text "Find the frequency of spatial cluster 10 in WHO grade 2,3 and 4 samples."
      :params {:universal {:dim :WHO_grade, :feature "spatial_clust_10", :filters {:WHO_grade {"2" true, "3" true, "4" true}}},
               :features {:feature-bio-feature-type nil, :feature-supertype "spatial", :scale "linear", :feature-broad_feature_type "Cells", :feature-feature_type "Spatial_Density", :feature-feature_variable "spatial_clust_10", :subfeature-0 "Macrophage_CD206", :subfeature-2 "Tcell_CD8"},
               :violin {"blobWidth" 100, "blobSpace" 530}}
      }

     {:text "What is the density of Myeloid CD11b+HLADR+ cells between GBM and PXA samples?"
      :params {:universal {:dim :Tumor_Diagnosis, :feature "Myeloid_CD11b_HLADRplus_density", :filters {:Tumor_Diagnosis {"GBM" true, "PXA" true}}}
               :features {:feature-bio-feature-type nil, :feature-supertype "spatial", :scale "linear", :feature-broad_feature_type "Cells", :feature-feature_type "Area_Density", :feature-feature_variable "Myeloid_CD11b_HLADRplus_density", :subfeature-0 "Macrophage_CD206", :subfeature-2 "Tcell_CD8"},
               :violin {"blobWidth" 100, "blobSpace" 530}}}
     
     {:text "CD163 transcript counts in GBM, Astrocytoma and Oligodendroglioma."
      :params {:universal {:dim :Tumor_Diagnosis,
                           :feature "CD163",
                           :filters {:Tumor_Diagnosis {"GBM" true, "PXA" false, "Astrocytoma" true, "Oligodendroglioma" true}}},
               :features {:feature-bio-feature-type nil, :feature-supertype "spatial", :scale "linear", :feature-broad_feature_type "RNA", :feature-feature_type "Immune_High", :feature-feature_variable "CD163", :subfeature-0 "Macrophage_CD206", :subfeature-2 "Tcell_CD8"},
               :violin {"blobWidth" 100, "blobSpace" 530},
               }}

     {:text "EGFR transcript counts in GBM, Astrocytoma and Oligodendroglioma"
      :params {:universal {:dim :WHO_grade,
                           :feature "EGFR",
                           :filters {:Tumor_Diagnosis {"GBM" true, "PXA" false, "Astrocytoma" true, "Oligodendroglioma" true}}},
               :features {:feature-bio-feature-type nil, :feature-supertype "spatial", :scale "linear", :feature-broad_feature_type "RNA", :feature-feature_type "Immune_Low", :feature-feature_variable "EGFR", :subfeature-0 "Macrophage_CD206", :subfeature-2 "Tcell_CD8"},
               :violin {"blobWidth" 100, "blobSpace" 530}
               }}

     {:text "B7H3+ tumor cells in WHO grade 2,3, & 4 samples"
      :params {:universal {:dim :WHO_grade, :feature "B7H3_func_over_all_tumor_count_prop", :filters {:WHO_grade {"2" true, "3" true, "4" true}}},
               :features {:feature-bio-feature-type "Relative_to_all_tumor_cells", :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Cells", :feature-feature_type "Cell_Abundance", :feature-feature_variable "B7H3_func_over_all_tumor_count_prop", :subfeature-0 "Macrophage_CD206", :subfeature-2 "Tcell_CD8"},
               :violin {"blobWidth" 100, "blobSpace" 530}}}

     {:text "CD14+ Myeloid cells in WHO grade 2,3, & 4 samples"
      :params {:universal {:dim :WHO_grade, :feature "Myeloid_CD14_over_all_immune_count_prop", :filters {:WHO_grade {"2" true, "3" true, "4" true}}},
               :features {:feature-bio-feature-type "Relative_to_all_immune_cells", :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Cells", :feature-feature_type "Cell_Abundance", :feature-feature_variable "Myeloid_CD14_over_all_immune_count_prop", :subfeature-0 "Macrophage_CD206", :subfeature-2 "Tcell_CD8"},
               :violin {"blobWidth" 100, "blobSpace" 530}
               }}

     {:text "What is the ratio of Macrophage CD68+ Ki67+ cells to B7H3+ tumor cells between male and female PXA samples?"
      :params {:universal {:dim :Sex, :feature "Macrophage_CD68_Ki67_over_Macrophage_CD68_Ki67_plus_B7H3_func", :filters {:Tumor_Diagnosis {"PXA" true}}},
               :features {:feature-broad_feature_type "Cells", :feature-feature_variable "Myeloid_CD14_over_all_immune_count_prop", :scale "linear", :feature-bio-feature-type "Cells_and_functional_markers", :feature-feature_type "Cell_Ratios", :subfeature-0 "Macrophage_CD68_Ki67", :subfeature-2 "Tcell_CD8", :subfeature-4 "B7H3", :feature-supertype "nonspatial"},
               :violin {"blobWidth" 100, "blobSpace" 530}
               }}

     {:text "What is the ratio of Macrophage CD68+ cells and endothelial cells in GBM sample core vs infiltrating edge regions?"
      :params {:universal {:dim :Tumor_Region, :feature "Endothelial_cells_over_Macrophage_CD68_plus_Endothelial_cells_prop", :filters {:Tumor_Diagnosis {"GBM" true}, :Tumor_Region {"Tumor_core" true, "Tumor_infiltrating" true}}},
               :features {:feature-broad_feature_type "Cells", :feature-feature_variable "Endothelial_cells_over_Macrophage_CD68_plus_Endothelial_cells_prop", :scale "linear", :feature-bio-feature-type "Immune_cells", :feature-feature_type "Cell_Ratios", :subfeature-0 "Macrophage_CD68_Ki67", :subfeature-2 "Tcell_CD8", :subfeature-4 "B7H3", :feature-supertype "nonspatial"},
               :violin {"blobWidth" 100, "blobSpace" 530}
               }}

     {:text "What is the ratio of Macrophage CD206+ cells and HER2+ tumor cells in GBM core vs infiltrating regions?"
      :params {:universal {:dim :Tumor_Region, :feature "Macrophage_CD206_over_HER2_func_plus_Macrophage_CD206_prop", :filters {:Tumor_Diagnosis {"GBM" true}, :Tumor_Region {"Tumor_core" true, "Tumor_infiltrating" true}}},
               :features {:feature-broad_feature_type "Cells", :feature-feature_variable "Macrophage_CD206_over_HER2_func_plus_Macrophage_CD206_prop", :scale "linear", :feature-bio-feature-type "Immune_to_Tumor_cells", :feature-feature_type "Cell_Ratios", :subfeature-0 "Macrophage_CD68_Ki67", :subfeature-2 "Tcell_CD8", :subfeature-4 "B7H3", :feature-supertype "nonspatial"},
               :violin {"blobWidth" 100, "blobSpace" 530}}}

     {:text "What is the ratio of tumor cells coexpressing B7H3 and EGFR vs tumor cells just expressing B7H3 in WHO grade 2,3 and 4 samples?"
      :params {:universal {:dim :WHO_grade, :feature "B7H3_func_EGFR_func_over_EGFR_func_prop", :filters {}},
               :features {:feature-broad_feature_type "Cells", :feature-feature_variable "B7H3_func_EGFR_func_over_EGFR_func_prop", :scale "linear", :feature-bio-feature-type "Tumor_cells", :feature-feature_type "Cell_Ratios", :subfeature-0 "Macrophage_CD68_Ki67", :subfeature-2 "Tcell_CD8", :subfeature-4 "B7H3", :feature-supertype "nonspatial"},
               :violin {"blobWidth" 100, "blobSpace" 530}}}

     {:text "Heatmap of all Cell Abundance relative to Immune Cell features" ;TODO 
      :params {:universal {:dim :Tumor_Diagnosis, :feature "Endothelial_cells_over_all_immune_count_prop"},
               :heatmap2 {:dim :Tumor_Diagnosis,
                          ;; TODO need a good way for users to make this list
                          :feature-list #{"APC_over_all_immune_count_prop" "Bcells_over_all_immune_count_prop" "DC_Mac_CD209_over_all_immune_count_prop" "Endothelial_cells_over_all_immune_count_prop" "Macrophage_CD206_over_all_immune_count_prop" "Macrophage_CD68_CD163_over_all_immune_count_prop" "Macrophage_CD68_over_all_immune_count_prop" "Mast_cells_over_all_immune_count_prop" "Microglia_CD163_over_all_immune_count_prop" "Microglia_over_all_immune_count_prop" "Myeloid_CD11b_HLADRminus_over_all_immune_count_prop" "Myeloid_CD11b_HLADRplus_over_all_immune_count_prop" "Myeloid_CD141_over_all_immune_count_prop" "Myeloid_CD14_CD163_over_all_immune_count_prop" "Myeloid_CD14_over_all_immune_count_prop" "Neurons_over_all_immune_count_prop" "Neutrophils_over_all_immune_count_prop" "Tcell_CD4_over_all_immune_count_prop" "Tcell_CD8_over_all_immune_count_prop" "Tcell_FoxP3_over_all_immune_count_prop" "Unassigned_over_all_immune_count_prop"}},
               :features {:feature-bio-feature-type "Relative_to_all_immune_cells", :feature-supertype "nonspatial", :scale "linear", :feature-broad_feature_type "Cells", :feature-feature_type "Cell_Abundance", :feature-feature_variable "Endothelial_cells_over_all_immune_count_prop"}}
      :active-tab {:uviz :heatmap}
      }
     ])

  (clojure.data.json/write-str examples-trimmed)
  )
