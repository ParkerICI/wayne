(ns wayne.qgen
  (:require [org.candelbio.multitool.core :as u]
            [clj-http.client :as client]
            [environ.core :as env]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [org.candelbio.multitool.nlp :as nlp]
            [wayne.data-defs :as dd]
            [taoensso.timbre :as log]
            [clojure.walk :as walk]
            [wayne.fastmatch :as fastmatch]
            [wayne.data :as data]
            ))

;;; Query generation. Takes an NLP string and generates a config map for the query interface.


;;; ☜⟐☞ basic plumbing ☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇

(defn openai-query
  [q]
  (client/post "https://api.openai.com/v1/chat/completions"
              {:as :json
               :headers {"Authorization" (str "Bearer " (env/env :openai-api-key))} 
               :content-type :application/json
               :body (json/write-str q)}))

(defn openai-json-query
  [q]
  (-> q
      openai-query
      (get-in [:body :choices 0 :message :content])
      (json/read-str :key-fn keyword)))


;;; ☜⟐☞ query and schema generation ☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇

;;; Adds the redundant but required elements to the json schema
(defn fixup-schema
  [schema]
  (walk/postwalk
   (fn [x]
     (if (and (map? x)
              (or (= :object (:type x))
                  (and (vector? (:type x))
                       (= :object (first (:type x))))))
       (assoc x
              :additionalProperties false
              :required (vec (keys (:properties x)))
              )
       x))
   schema))
   
(def json-format
  (fixup-schema
   {:type "json_schema"
    :json_schema {:name "Query_configuration"
                  :strict true
                  :schema {:type :object
                           :properties
                           {:text {:type :string
                                   :description "The user's natural language query spec"}
                            :params {:type :object
                                     :properties
                                     {:universal {:type :object
                                                  :properties
                                                  {:dim {:type :string
                                                         :description "The main dimension to compare across"
                                                         :enum (vec (keys dd/dims))}
                                                   ;; Redudant with :features/feature-feature_variable
                                                   ;; :feature {:type :string
                                                   ;; :description "The feature to plot, eg EGFR level"}
                                                   :filters {:type :object
                                                            :properties (u/map-values (fn [{:keys [label info values]}]
                                                                                        {:type [:object :null] ;Union type, a neat trick
                                                                                         :description (str label " " info)
                                                                                         :properties (into {} (map (fn [v] [(if (vector? v) (first v) v) {:type :boolean}])) values)
                                                                                         })
                                                                                      dd/dims)}}}
                                      :heatmap2 {:type :object
                                                 :properties
                                                 {:dim {:type :string
                                                        :description "The main dimension to compare across"
                                                        :enum (vec (keys dd/dims))}}
                                                 }
                                      :features {:type :object
                                                 :properties {:feature-supertype {:type :string
                                                                                  :enum (dd/features 0)}
                                                              :feature-broad_feature_type {:type :string
                                                                                           :enum (dd/features 1)}
                                                              :feature-feature_type {:type :string
                                                                                     :enum (dd/features 2)}
                                                              :feature-bio-feature-type {:type :string
                                                                                         :enum (dd/features 3)}
                                                              :feature-feature_variable {:type :string
                                                                                         ;; too many to enumerate unfortunately
                                                                                         :description "The variable to be plotted"}
                                                              }}
                                      ;; No real point to asking for these
                                      #_ :violin #_ {:type :object
                                               :properties {:blobWidth {:type :number :description "blob width, default 100"}
                                                            :blobSpace {:type :number :description "blob spacing, default 500"}}}
                                      
                                      }}}}}}))

;;; ☜⟐☞ result fixups☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇

(defn fixup-filters
  [fs]
  (->> fs
       (u/map-values (fn [fsv] (u/map-keys name fsv)))
       (u/dissoc-if (fn [[k v]] (every? true? (vals v))))))

(u/def-lazy index (fastmatch/build-name-index @data/feature-variables))

(defn recognize-feature
  [s]
  (if-let [match (fastmatch/find-best-match @index s)]
    (:name match)
    s))                                 ;no match, return original

(defn fixup-query
  [q]
  (-> q
      (update-in [:params :universal :dim] keyword)
      (update-in [:params :universal :filters] fixup-filters)
      (update-in [:params :features :feature-feature_variable] recognize-feature)))

;;; ☜⟐☞ endpoint ☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇☜⟐☞⧇

(defn openai-example-query
  [text]
  (->
   (openai-json-query
    {:model "gpt-4o"
     :messages [{:role :system :content "you are a computational cancer biolologist. "}
                {:role :user :content (str "Please translate this query to JSON, compute the params given the text: " text)}
                ;; Not clear if these are needed or good...needs more experiments
                {:role :user :content "Sample JSON queries [{\"text\":\"Find relative intensity of fucosylated glycans in WHO grade 2,3 and 4 samples. \",\"params\":{\"universal\":{\"dim\":\"WHO_grade\",\"feature\":\"fucosylated\"},\"features\":{\"feature-bio-feature-type\":null,\"feature-supertype\":\"nonspatial\",\"scale\":\"linear\",\"feature-broad_feature_type\":\"Glycan\",\"feature-feature_type\":\"Relative_Intensity\",\"feature-feature_variable\":\"fucosylated\"},\"violin\":{\"blobWidth\":100,\"blobSpace\":328}}},
{\"text\":\"Find mean intensity of EGFR expression on tumor cells in primary WHO grade 3 and 4 samples.\",\"params\":{\"universal\":{\"dim\":\"WHO_grade\",\"feature\":\"EGFR\",\"filters\":{\"recurrence\":{\"No\":true},\"WHO_grade\":{\"3\":true,\"4\":true}}},\"features\":{\"feature-bio-feature-type\":null,\"feature-supertype\":\"nonspatial\",\"scale\":\"linear\",\"feature-broad_feature_type\":\"Protein\",\"feature-feature_type\":\"Tumor_Antigens_Intensity\",\"feature-feature_variable\":\"EGFR\"},\"violin\":{\"blobWidth\":100,\"blobSpace\":328}}},
{\"text\":\"Find mean intensity of TOX expression in recurrent GBM samples and compare between control and Neoadjuvant PD1 Trial 1\",\"params\":{\"universal\":{\"dim\":\"treatment\",\"feature\":\"Tox\",\"filters\":{\"Tumor_Diagnosis\":{\"GBM\":true},\"treatment\":{\"Neoadjuvant_PD1_Trial_1\":true,\"Treatment_Naive\":true},\"recurrence\":{\"Yes\":true}}},\"features\":{\"feature-bio-feature-type\":null,\"feature-supertype\":\"nonspatial\",\"scale\":\"linear\",\"feature-broad_feature_type\":\"Protein\",\"feature-feature_type\":\"Functional_marker_intensity\",\"feature-feature_variable\":\"Tox\"},\"heatmap\":{\"filter\":{}}}}]"}
                ]
     :response_format json-format
     :stream false})
   fixup-query
   ))

(defn endpoint
  [query]
  (log/info "Query gen" query)
  (let [res (-> query
                openai-example-query)]
    (log/info "Query gen" query res)
    res))


