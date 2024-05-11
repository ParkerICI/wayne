(ns wayne.heatmap-data
  (:require [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.math :as mu]
            [clojure.string :as str]))

;;; https://bioinformatics.ccr.cancer.gov/docs/btep-coding-club/CC2023/complex_heatmap_enhanced_volcano/
;;; https://en.wikipedia.org/wiki/Ward%27s_method

;;; Not loaded in running system

#_
(defn write-json-file [f content]
  (with-open [s (clojure.java.io/writer f)]
    (clojure.data.json/write content s)))

;;; TODO in multitool
(defn median
  [seq]
  (let [sorted (sort seq)
        count (count seq)]
    (if (even? count)
      (mu/mean [(nth sorted (/ count 2))
               (nth sorted (dec (/ count 2)))])
      (nth sorted (/ (dec count) 2)))))

;;: TODO ??? What's wrong with multitool versions, argh. Context?
(def ^:dynamic *side-walk-context* ())

(defn side-walk
  "Walks form, an arbitrary data structure, evaluating f on each element for side effects. Note: has nothing to do with the standard (functional) walker, and maybe should have a different name (traverse?)"
  [f form]
  (do 
    (f form)
    (binding [*side-walk-context* (cons form *side-walk-context*)]
      (cond
        (coll? form) (doseq [elt form] (side-walk f elt))
        (map-entry? form)
        (do (side-walk f (key form))
            (side-walk f (val form)))))))

(defn walk-reduce
  "Walks form with an accumulator. f is a function of [accumulator elt], init is initial val of accumulator."
  [f form init]
  (let [acc (atom init)]          ;typically acc should be transient, but since they need special mutators can't be done in a general way. See walk-collect below
    (side-walk
     (fn [elt]
       (swap! acc f elt))
     form)
    @acc))

(defn walk-collect
  "Walk f over thing and return a list of the non-nil returned values"
  [f thing]
  (persistent!
   (walk-reduce (fn [acc elt]
                 (if-let [it (f elt)]
                   (conj! acc it)
                   acc))
               thing
               (transient []))))

(defn id-generator
  []
  (let [next (atom 0)                   ;I suppose should be just one atom
        cache (atom {})]
    (fn [thing]
      (or (get @cache thing)
          (do
            (let [id (swap! next inc)]
              (swap! cache assoc thing id)
              id))))))


;;; Replication baybe



(defn rename-key
  [old new map]
  (-> map
      (assoc new (get map old))
      (dissoc old)))

;;; Why am I doing this
(defn unpivot
  [rows idcol idcol2 colcol valcol]
  (mapcat (fn [row]
            (map (fn [[k v]] {idcol2 (get row idcol)
                              colcol k
                              valcol v})
                 (dissoc row idcol)))
          rows))


(defn square [x] (* x x))

(defn manhattan-distance
  [v1 v2]
  (reduce + (map (comp abs -) v1 v2)))

(defn euclidean-distance
  [v1 v2]
  (Math/sqrt (reduce + (map (comp square -) v1 v2))))

(defn euclidean-squared-distance
  [v1 v2]
  (reduce + (map (comp square -) v1 v2)))

(def vector-mean (u/vectorize (fn [a b] (/ (+ a b) 2))))

;;; Seriously, the one in multitoool doesn't do E notatioN???
(defn coerce-numeric
  [v]
  (if (double? v)
    v
    #?(:cljs (js/parseFloat v)
       :clj (Double. v))
    ))
  
(defn index-by-transform 
  "Return a map of the elements of coll indexed by (f elt). Similar to group-by, but overwrites elts with same index rather than producing vectors. "
  [f xform coll]  
  (zipmap (map f coll) (map xform coll)))

;;; Naive and inefficient algo
;;; maps: data as seq of maps
;;; idcol: dimension to be clustered
;;; → multitool

;;; For clarity, clustering rows by columns (but actually works on mapseqs so any field can be "row" or "column")
;;; row-dim: the field containing the leaf clusters (rows)
;;; col-dim: the other dimension (columns)
;;; value-field: field containing values to be clustered
(defn cluster
  [maps row-dim col-dim value-field]
  (let [indexed (index-by-transform
                 (juxt row-dim col-dim)
                 (comp coerce-numeric value-field)
                 maps)            ;produces essentially a matrix, what clustring usually starts with
        rows (distinct (map row-dim maps))
        cols (distinct (map col-dim maps))
        vectors (zipmap rows (map (fn [row]
                                   (vec (map (fn [col]
                                               (get indexed [row col]))
                                             cols)))
                                 rows))
        ;; Initialize to the complete graph of inter-row distances
        distances (into {}
                        (u/forf [row1 rows row2 rows]
                          (when (u/<* row1 row2)
                            [[row1 row2] (euclidean-squared-distance (get vectors row1) (get vectors row2))])))
        ]
    (loop [vectors vectors              ;TODO make transient, but some issues
           distances distances
           tree []]
      (if (= 1 (count vectors))
        tree
        (let [[[row1 row2] _] (u/min-by second distances)
              cluster-id (str row1 "-" row2) ;TODO These IDS are annoyingly large, good for debugging though
              vector (vector-mean (get vectors row1) (get vectors row2))]
          (recur (-> vectors            ;replace merged (clustered) vectors with new one (mean).
                     (dissoc row1 row2)
                     (assoc cluster-id vector))
                 (->> distances         ;remove 
                      (u/dissoc-if (fn [[[xrow1 xrow2] _]]
                                     (or (= row1 xrow1) (= row1 xrow2)
                                         (= row2 xrow1) (= row2 xrow2))))
                      (merge (into {}
                                   (for [[id v] (dissoc vectors row1 row2)] ;TODO doing this twice
                                     [[id cluster-id] (euclidean-squared-distance v vector)]))))

                 (conj tree [cluster-id row1 row2])
                 ))))))

;;; Returns a mapseq of {:id :parent}, suitable for passing to vega tree.
(defn cluster-data
  [maps row-dim col-dim value-field]
  (let [clusters (cluster maps row-dim col-dim value-field)
        invert (merge (u/index-by second clusters) (u/index-by #(nth % 2)  clusters))
        root (last clusters)]
    (cons {:id (first root)}
          (map (fn [[c [p _]]]
                 {:id c :parent p})
               invert))))


;;; https://github.com/lerouxrgd/clj-hclust
;;; https://github.com/tyler/clojure-cluster

