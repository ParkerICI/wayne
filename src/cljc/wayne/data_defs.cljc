(ns wayne.data-defs
  )

(def dims
  (array-map                            ; Order is important 
   :Tumor_Diagnosis {:label "Tumor Diagnosis"
                     :info "Glial tumor subtypes"
                     :icon "diagnosis-icon.svg"
                     :values ["Astrocytoma"
                              "GBM"
                              "Oligodendroglioma"
                              "PXA"
                              "Pediatric DIPG"
                              "Pediatric HGG (other)"]}
   :WHO_grade {:label "WHO Grade"
               :info "World Health Organization tumor grade classification"
               :icon "question-icon.svg"
               :values  ["2" "3" "4" "Unknown"]}
   :Immunotherapy {:label "Immunotherapy"
                   :type :boolean
                   :icon "cell-therapy-2.png"
                   :info "Treatment status"
                   :values [["false" "No"] ["true" "Yes"]]}
   :treatment {:label "Treatment"
               :info "Various pre-sample collection treatments"
               :icon "treatment-icon.svg"
               :values ["Combinatorial_CD27_and_SPORE_Vaccine"
                        "Lysate_Vaccine"
                        "Neoadjuvant_PD1_Trial_1"
                        "Neoadjuvant_PD1_Trial_2"
                        "SPORE_Vaccine"
                        "Treatment_Naive"]}
   :recurrence {:label "Recurrence"
                :info "Recurrent tumor"
                :icon "recurrence-icon.svg"
                :values ["No" "Yes"]}
   :Longitudinal {:label "Longitudinal"
                  :icon "time_b.png"
                  :info "Patient samples with paired primary and recurrent events"
                  :values ["Yes" "No"]}
   :Progression {:label "Progression"
                 :icon "data.png"
                 :info "Patients that progressed from lower grade to higher grades. later event catagories denotes the recurrent tumor."
                 :values ["No" "No_later_event" "Yes" "Yes_later_event"]}
   :Tumor_Region {:label "Tumor Region"
                  :info "Anatomical regions of the tumor"
                  :icon "roi-icon.svg"
                  :values   ["Other" "Tumor_core" "Tumor_core_to_infiltrating" "Tumor_infiltrating"]}
   :IDH_R132H_Status {:label "IDH Status"
                      :info "R132H - Common IDH mutation"
                      :icon "file-chart-icon.svg"
                      :values ["Mutant" "Wild_type"]
                      }
   :Sex {:label "Sex"
         :icon "gender.png"
         :values [["F" "Female"] ["M" "Male"] "Unknown"]}
   ))
