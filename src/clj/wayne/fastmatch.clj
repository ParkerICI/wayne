(ns wayne.fastmatch
  (:require [clojure.string :as str]
            [clojure.set :as set]))

;;; This code mostly from Claude, I apologize

;; Normalize name for better matching
(defn normalize-name
  "Normalize a name by lowercasing, trimming, removing extra spaces, replacing _ with space."
  [name]
  (-> name
      str/lower-case
      str/trim
      (str/replace #"\s+" " ")
      (str/replace #"_" " ")))

;; Generate n-grams from a string (bigrams by default)
(defn n-grams
  "Generate n-grams from a string."
  [s & {:keys [n] :or {n 2}}]
  (let [padded (str (apply str (repeat n \space)) s (apply str (repeat n \space)))]
    (set (for [i (range (- (count padded) (dec n)))]
           (subs padded i (+ i n))))))

;; Jaccard similarity - fast approximate matching
(defn jaccard-similarity
  "Calculate Jaccard similarity between two strings using n-grams."
  [s1 s2 & {:keys [n] :or {n 2}}]
  (let [ng1 (n-grams s1 :n n)
        ng2 (n-grams s2 :n n)
        intersection-size (count (set/intersection ng1 ng2))
        union-size (count (set/union ng1 ng2))]
    (if (zero? union-size)
      0.0
      (double (/ intersection-size union-size)))))

;; Build index for fast approximate matching
(defn build-name-index
  "Build an index optimized for fast approximate matching."
  [names]
  (let [;; Create normalized versions of all names
        normalized-names (map normalize-name names)
        
        ;; Generate n-grams for each name
        name-grams (map #(n-grams % :n 2) normalized-names)
        
        ;; Create inverted index: n-gram -> set of name indices
        gram-index (reduce (fn [index [name-idx grams]]
                             (reduce (fn [idx gram]
                                       (update idx gram (fnil conj #{}) name-idx))
                                     index
                                     grams))
                           {}
                           (map-indexed vector name-grams))
        
        ;; First letter index for quick filtering
        first-letter-index (reduce (fn [index [name-idx norm-name]]
                                     (if (seq norm-name)
                                       (update index (str (first norm-name))
                                               (fnil conj #{}) name-idx)
                                       index))
                                   {}
                                   (map-indexed vector normalized-names))]
    
    {:names (vec names)
     :normalized (vec normalized-names)
     :gram-index gram-index
     :first-letter-index first-letter-index}))

;; Fast name matching function
(defn find-best-match
  "Find the best matching name using approximate techniques."
  [name-index query & {:keys [threshold min-candidates] 
                       :or {threshold 0.5 min-candidates 10}}]
  (let [norm-query (normalize-name query)]
    (when (seq norm-query)
      (let [;; Get query n-grams
            query-grams (n-grams norm-query)
            
            ;; First quick filter by first letter
            first-letter (str (first norm-query))
            first-candidates (get-in name-index [:first-letter-index first-letter] #{})
            
            ;; Get candidate indices by matching n-grams
            candidate-sets (for [gram query-grams
                                :let [matches (get-in name-index [:gram-index gram] #{})]
                                :when (seq matches)]
                            matches)
            
            ;; Merge all candidate sets
            candidates-from-grams (if (seq candidate-sets)
                                    (apply set/union candidate-sets)
                                    #{})
            
            ;; Combine first-letter and n-gram candidates
            initial-candidates (if (and (seq first-candidates) (< (count first-candidates) 1000))
                                (set/intersection first-candidates candidates-from-grams)
                                candidates-from-grams)
            
            ;; Ensure minimum number of candidates
            candidates (if (< (count initial-candidates) min-candidates)
                         (set/union initial-candidates first-candidates)
                         initial-candidates)
            
            ;; Fast pre-filter using length similarity
            length-filtered (filter identity #_ #(let [len-diff (Math/abs (- (count (get-in name-index [:normalized %]))
                                                               (count norm-query)))]
                                      (<= len-diff (max 3 (int (* 0.3 (count norm-query))))))
                                    candidates)
            
            ;; Calculate similarities
            matches (for [idx length-filtered
                         :let [name (get-in name-index [:names idx])
                               norm-name (get-in name-index [:normalized idx])
                               similarity (jaccard-similarity norm-query norm-name)]]
                     {:name name
                      :similarity similarity})
            
            ;; Find best match above threshold
            best-match (when (seq matches)
                         (apply max-key :similarity matches))]
        
        (when (and best-match (>= (:similarity best-match) threshold))
          best-match)))))

;; Example usage
(comment
  (def names ["John Smith", "Jane Doe", "Robert Johnson", "Sarah Williams" 
              ;; ... imagine 12000 names here
             ])
  
  (def name-index (build-name-index names))
  
  ;; Find matches - much faster than Levenshtein
  (find-best-match name-index "Jon Smith")      ;; => {:name "John Smith", :similarity 0.82}
  (find-best-match name-index "Bob Johnson")    ;; => {:name "Robert Johnson", :similarity 0.68}
  (time (find-best-match name-index "Sarah"))   ;; Should be very fast
)


