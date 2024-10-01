#_
(defn write-json-file [f content]
  (with-open [s (clojure.java.io/writer f)]
    (clojure.data.json/write content s)))

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


(defn recurrence1
  [{:keys [recurrence Tumor_Diagnosis] :as row}]
  (assoc row
         :Recurrence1
         (cond (= Tumor_Diagnosis "Normal_brain") "Normal_brain"
               (= "no" recurrence) "Primary"
               (= "yes" recurrence) "Recurrence"
               :else "OTHER")))

(comment
(def x2 (select "feature_variable, feature_value, recurrence, Tumor_Diagnosis {from} where feature_variable = 'EGFR_func_over_all_tumor_prop'"))
(def x2x (map recurrence1 x2))

(def x3 (map recurrence1 (select (format "feature_variable, feature_value, recurrence, Tumor_Diagnosis {from}
 where feature_variable in %s" (wu/sql-lit-list ["EGFR_func_over_all_tumor_prop" "GM2_GD2_func_over_all_tumor_prop"])))))
)

#_
(defn x3p
  [features]
  (map recurrence1 (select (format "feature_variable, feature_value, recurrence, Tumor_Diagnosis {from}
 where feature_variable in %s" (wu/sql-lit-list features)))))

(def features1 ["EGFR_func_over_all_tumor_prop"
                "GM2_GD2_func_over_all_tumor_prop"
                "GPC2_func_over_all_tumor_prop"
                "VISTA_func_over_all_tumor_prop"
                "HER2_func_over_all_tumor_prop"
                "B7H3_func_over_all_tumor_prop"
                "NG2_func_over_all_tumor_prop"
                ])


#_
(def x3 (x3p features1))

;;; Note: this is probably wrong, the R code does median per-patient or per-sample or something. But good enough for our purposes I guesss
#_
(def x3a (map (fn [[k v]]
                (assoc (first v)
                       :feature_value
                       (median (map (fn [s] (Double. (:feature_value s))) v))))
              (group-by (juxt :feature_variable :recurrence1) x3)))

#_
(write-json-file "resources/public/hm2.json" x3a)



(def fake-tree-1
  '[[egfr gm2_gd2]
    [[[gpc2 vista]
      her2]
     [b7h3 ng2]]])

(def fake-tree-2
  '[[Primary Recurrence]
    Normal_brain])

(comment
(write-json-file "resources/public/dend1.json" (write-real-tree fake-tree-1))
(write-json-file "resources/public/dend2.json" (write-real-tree fake-tree-2))

(write-real-tree fake-tree-1)
)

#_
(def top20 (map (partial rename-key "" :gene)
                (read-csv-maps "/Users/mt/Downloads/data/RNAseq_mat_top20.csv")))

(comment
(def top20up (-> "/Users/mt/Downloads/data/RNAseq_mat_top20.csv"
                 read-csv-maps
                 (unpivot "" :gene :sample :value)))

(write-json-file "resources/public/sheatmap.json" top20up)
)

(defn write-real-tree
  [fake]
  (let [id-gen (id-generator)]
    (walk-collect
     (fn [x]
       {:id (id-gen x)
        :name (str x)
        :parent (when  (first *side-walk-context*)
                  (id-gen (first *side-walk-context*)))})
     fake)))

#_(write-clusters "resources/public/dend-real-s.json" (cluster top20up :sample :gene :value))
#_(write-clusters "resources/public/dend-real-g.json" (cluster top20up :gene :sample :value))




;;; https://github.com/lerouxrgd/clj-hclust
;;; https://github.com/tyler/clojure-cluster

(defn read-csv-rows
  "Read a tsv file into vectors"
  [f]
  (map #(str/split % #"\,")
       (ju/file-lines f)))

(defn read-csv-maps
  "Given a tsv file with a header line, returns seq where each elt is a map of field names to strings"
  [f]
  (let [rows (read-csv-rows f)]
    (map #(zipmap (first rows) %)
         (rest rows))))

(def raw (read-csv-maps "/Users/mt/Downloads/data/RNAseq_mat_top20.csv"))

(def maps (mapcat (fn [row]
                    (let [gene (get row "")]
                      (for [[samp v] (dissoc row "")]
                        {:gene gene :sample samp :value v})))
                  raw))



(def g-clusters '({:id
  "CACNB2-SPARCL1-HIF3A-TIMP4-FAM107A-ZBTB16-WNT2-PRSS35-VCAM1-ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "WNT2-PRSS35-VCAM1",
  :parent "WNT2-PRSS35-VCAM1-ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "ACSS1-PDPN", :parent "ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "PDPN", :parent "ACSS1-PDPN"}
 {:id "MT2A-STEAP2", :parent "DUSP1-MT2A-STEAP2"}
 {:id "VCAM1", :parent "PRSS35-VCAM1"}
 {:id "WNT2", :parent "WNT2-PRSS35-VCAM1"}
 {:id "STEAP2", :parent "MT2A-STEAP2"}
 {:id "CACNB2", :parent "CACNB2-SPARCL1"}
 {:id "DNAJB4", :parent "DNAJB4-FGD4"}
 {:id "DUSP1", :parent "DUSP1-MT2A-STEAP2"}
 {:id "HIF3A", :parent "HIF3A-TIMP4"}
 {:id "ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4",
  :parent "WNT2-PRSS35-VCAM1-ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "ACSS1", :parent "ACSS1-PDPN"}
 {:id "ADAM12-DNM1", :parent "ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "FGD4", :parent "DNAJB4-FGD4"}
 {:id "SPARCL1", :parent "CACNB2-SPARCL1"}
 {:id "ACSS1-PDPN-MAOA-DNAJB4-FGD4", :parent "NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "MT2A", :parent "MT2A-STEAP2"}
 {:id "DNM1", :parent "ADAM12-DNM1"}
 {:id "PRSS35", :parent "PRSS35-VCAM1"}
 {:id "DNAJB4-FGD4", :parent "MAOA-DNAJB4-FGD4"}
 {:id "WNT2-PRSS35-VCAM1-ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4",
  :parent
  "CACNB2-SPARCL1-HIF3A-TIMP4-FAM107A-ZBTB16-WNT2-PRSS35-VCAM1-ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "ZBTB16", :parent "FAM107A-ZBTB16"}
 {:id "FAM107A", :parent "FAM107A-ZBTB16"}
 {:id "NEXN", :parent "NEXN-DUSP1-MT2A-STEAP2"}
 {:id "CACNB2-SPARCL1", :parent "CACNB2-SPARCL1-HIF3A-TIMP4"}
 {:id "DUSP1-MT2A-STEAP2", :parent "NEXN-DUSP1-MT2A-STEAP2"}
 {:id "TIMP4", :parent "HIF3A-TIMP4"}
 {:id "CACNB2-SPARCL1-HIF3A-TIMP4", :parent "CACNB2-SPARCL1-HIF3A-TIMP4-FAM107A-ZBTB16"}
 {:id "CACNB2-SPARCL1-HIF3A-TIMP4-FAM107A-ZBTB16",
  :parent
  "CACNB2-SPARCL1-HIF3A-TIMP4-FAM107A-ZBTB16-WNT2-PRSS35-VCAM1-ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "HIF3A-TIMP4", :parent "CACNB2-SPARCL1-HIF3A-TIMP4"}
 {:id "MAOA", :parent "MAOA-DNAJB4-FGD4"}
 {:id "NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4",
  :parent "ADAM12-DNM1-NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "ADAM12", :parent "ADAM12-DNM1"}
 {:id "MAOA-DNAJB4-FGD4", :parent "ACSS1-PDPN-MAOA-DNAJB4-FGD4"}
 {:id "PRSS35-VCAM1", :parent "WNT2-PRSS35-VCAM1"}
 {:id "FAM107A-ZBTB16", :parent "CACNB2-SPARCL1-HIF3A-TIMP4-FAM107A-ZBTB16"}
                  {:id "NEXN-DUSP1-MT2A-STEAP2", :parent "NEXN-DUSP1-MT2A-STEAP2-ACSS1-PDPN-MAOA-DNAJB4-FGD4"}))

(def s-clusters
  '({:id "516-520-508-512-517-513-509-521"}
 {:id "508", :parent "508-512"}
 {:id "520", :parent "520-508-512"}
 {:id "509-521", :parent "513-509-521"}
 {:id "513-509-521", :parent "517-513-509-521"}
 {:id "521", :parent "509-521"}
 {:id "512", :parent "508-512"}
 {:id "508-512", :parent "520-508-512"}
 {:id "517-513-509-521", :parent "516-520-508-512-517-513-509-521"}
 {:id "520-508-512", :parent "516-520-508-512"}
 {:id "509", :parent "509-521"}
 {:id "517", :parent "517-513-509-521"}
 {:id "516", :parent "516-520-508-512"}
 {:id "513", :parent "513-509-521"}
    {:id "516-520-508-512", :parent "516-520-508-512-517-513-509-521"}))

(def movies
  '({:distributor "Eros Entertainment", :genre "Drama", :gross 369744}
 {:distributor "Apparition", :genre "Thriller/Suspense", :gross 406216}
 {:distributor "Sony/TriStar", :genre "Action", :gross 253526560}
 {:distributor "Outrider Pictures", :genre "Drama", :gross 174682}
 {:distributor "Grammercy", :genre "Black Comedy", :gross 3897569}
 {:distributor "20th Century Fox", :genre nil, :gross 214487000}
 {:distributor "Romar", :genre "Action", :gross 2405420}
 {:distributor "Walt Disney Pictures", :genre "Adventure", :gross 7104357206}
 {:distributor "Magnolia Pictures", :genre "Drama", :gross 11275553}
 {:distributor "United Artists", :genre "Musical", :gross 80500000}
 {:distributor nil, :genre "Black Comedy", :gross 20987}
 {:distributor "Indican Pictures", :genre "Documentary", :gross 162605}
 {:distributor "First Run/Icarus", :genre "Documentary", :gross 7033}
 {:distributor "Dreamworks SKG", :genre "Comedy", :gross 580930798}
 {:distributor "Power Point Films", :genre "Drama", :gross 2335352}
 {:distributor "Samuel Goldwyn Films", :genre "Action", :gross 96793}
 {:distributor "Artistic License", :genre nil, :gross 444354}
 {:distributor "Universal", :genre "Documentary", :gross 2739450}
 {:distributor "Focus/Rogue Pictures", :genre "Horror", :gross 17016190}
 {:distributor "Weinstein Co.", :genre "Adventure", :gross 22711709}
 {:distributor "Regent Releasing", :genre "Drama", :gross 95204}
 {:distributor "Vitagraph Films", :genre nil, :gross 476270}
 {:distributor "Senator Entertainment", :genre "Thriller/Suspense", :gross 315000}
 {:distributor "Focus/Rogue Pictures", :genre "Thriller/Suspense", :gross 10047674}
 {:distributor "IDP Distribution", :genre "Documentary", :gross 11529368}
 {:distributor "Focus Features", :genre "Horror", :gross 0}
 {:distributor "Sony Pictures Classics", :genre "Drama", :gross 303561296}
 {:distributor "Testimony Pictures", :genre "Drama", :gross 32092}
 {:distributor "Anchor Bay Entertainment", :genre "Drama", :gross 4354546}
 {:distributor "Miramax", :genre "Documentary", :gross 144601}
 {:distributor "Universal", :genre "Action", :gross 3201752892}
 {:distributor "Here Films", :genre nil, :gross 0}
 {:distributor "RKO Radio Pictures", :genre "Drama", :gross 27600000}
 {:distributor nil, :genre "Adventure", :gross 49959560}
 {:distributor "Picturehouse", :genre "Drama", :gross 13528849}
 {:distributor "Artisan", :genre "Adventure", :gross 25571351}
 {:distributor "Miramax", :genre "Romantic Comedy", :gross 554366808}
 {:distributor "Truly Indie", :genre "Thriller/Suspense", :gross 70071}
 {:distributor "MGM", :genre "Western", :gross 26335128}
 {:distributor "MGM", :genre "Thriller/Suspense", :gross 218791803}
 {:distributor "Sony Pictures Classics", :genre "Comedy", :gross 46214067}
 {:distributor nil, :genre nil, :gross 1779261550}
 {:distributor "Painted Zebra Releasing", :genre "Horror", :gross 126021}
 {:distributor "Paramount Pictures", :genre "Documentary", :gross 617172}
 {:distributor nil, :genre "Horror", :gross 93733776}
 {:distributor "20th Century Fox", :genre "Romantic Comedy", :gross 549916841}
 {:distributor "Orion Pictures", :genre "Drama", :gross 178089643}
 {:distributor "Excel Entertainment", :genre "Thriller/Suspense", :gross 852206}
 {:distributor "IFC Films", :genre "Documentary", :gross 3121270}
 {:distributor "Providence Entertainment", :genre "Action", :gross 12610552}
 {:distributor "New Line", :genre "Drama", :gross 899352321}
 {:distributor "Off-Hollywood Distribution", :genre "Comedy", :gross 16137}
 {:distributor "Warner Bros.", :genre nil, :gross 22378650}
 {:distributor "Weinstein/Dimension", :genre "Horror", :gross 78744577}
 {:distributor "Indican Pictures", :genre "Drama", :gross 6260}
 {:distributor "Focus Features", :genre "Drama", :gross 408469358}
 {:distributor "Giant Screen Films", :genre "Documentary", :gross 18642318}
 {:distributor "Walt Disney Pictures", :genre "Western", :gross 261459078}
 {:distributor "Third Rail", :genre "Adventure", :gross 166003}
 {:distributor "Rocky Mountain Pictures", :genre "Drama", :gross 13395961}
 {:distributor "Paramount Pictures", :genre "Horror", :gross 608186619}
 {:distributor "Universal", :genre "Western", :gross 29100000}
 {:distributor "Polygram", :genre "Adventure", :gross 11156471}
 {:distributor "Attitude Films", :genre "Drama", :gross 16556}
 {:distributor "Orion Pictures", :genre "Comedy", :gross 92371714}
 {:distributor "Sony/Columbia", :genre "Comedy", :gross 78097478}
 {:distributor "Walt Disney Pictures", :genre "Documentary", :gross 19422319}
 {:distributor "Polygram", :genre "Comedy", :gross 22619589}
 {:distributor "Zeitgeist", :genre nil, :gross 44705}
 {:distributor "Sony Pictures", :genre nil, :gross 30297823}
 {:distributor "Warner Bros.", :genre "Documentary", :gross 6706368}
 {:distributor "Polygram", :genre "Drama", :gross 55485043}
 {:distributor "Roxie Releasing", :genre nil, :gross 12836}
 {:distributor "Focus Features", :genre "Thriller/Suspense", :gross 67027491}
 {:distributor "Cowboy Pictures", :genre nil, :gross 69582}
 {:distributor "20th Century Fox", :genre "Western", :gross 147035544}
 {:distributor "National Geographic Entertainment", :genre "Concert/Performance", :gross 10363341}
 {:distributor "Sony/TriStar", :genre "Horror", :gross 11603545}
 {:distributor "Miramax/Dimension", :genre "Comedy", :gross 428416987}
 {:distributor "Alliance", :genre "Comedy", :gross 16150602}
 {:distributor "Sony Pictures Classics", :genre "Thriller/Suspense", :gross 15651917}
 {:distributor "Anchor Bay Entertainment", :genre "Comedy", :gross 115879}
 {:distributor "Focus Features", :genre "Black Comedy", :gross 60355347}
 {:distributor "Lionsgate", :genre "Drama", :gross 323034611}
 {:distributor "IDP Distribution", :genre "Drama", :gross 1110186}
 {:distributor "New World", :genre "Action", :gross 210904}
 {:distributor "United Artists", :genre "Thriller/Suspense", :gross 83077470}
 {:distributor "Cannon", :genre "Horror", :gross 12910535}
 {:distributor "USA Films", :genre "Comedy", :gross 55177079}
 {:distributor nil, :genre "Action", :gross 12898071}
 {:distributor "Present Pictures/Morning Knight", :genre "Drama", :gross 28835}
 {:distributor "Weinstein Co.", :genre "Thriller/Suspense", :gross 54622087}
 {:distributor "Goodbye Cruel Releasing", :genre "Comedy", :gross 180579}
 {:distributor "Palm Pictures", :genre nil, :gross 134624}
 {:distributor "New Line", :genre "Adventure", :gross 1499971782}
 {:distributor "Gramercy", :genre "Romantic Comedy", :gross 52700832}
 {:distributor "8X Entertainment", :genre "Action", :gross 6047691}
 {:distributor "Filmways Pictures", :genre "Thriller/Suspense", :gross 13747234}
 {:distributor "Fox Searchlight", :genre nil, :gross 309884}
 {:distributor "Truly Indie", :genre nil, :gross 84689}
 {:distributor "Newmarket Films", :genre "Comedy", :gross 5853194}
 {:distributor "Weinstein Co.", :genre "Horror", :gross 17138968}
 {:distributor "MGM", :genre nil, :gross 372179102}
 {:distributor "Summit Entertainment", :genre "Action", :gross 24850922}
 {:distributor "CBS Films", :genre "Romantic Comedy", :gross 37490007}
 {:distributor "National Geographic Entertainment", :genre nil, :gross 0}
 {:distributor "Rosebud Releasing", :genre "Horror", :gross 5923044}
 {:distributor "Lionsgate", :genre "Black Comedy", :gross 21596047}
 {:distributor "Fine Line", :genre "Musical", :gross 7224803}
 {:distributor "Polygram", :genre "Thriller/Suspense", :gross 48265581}
 {:distributor "Miramax/Dimension", :genre "Action", :gross 88095028}
 {:distributor "Focus/Rogue Pictures", :genre "Drama", :gross 2075743}
 {:distributor "Sony Pictures", :genre "Musical", :gross 98125385}
 {:distributor "Sony Pictures Classics", :genre "Adventure", :gross 7689458}
 {:distributor "Weinstein/Dimension", :genre "Comedy", :gross 105996208}
 {:distributor "Access Motion Picture Group", :genre "Drama", :gross 638227}
 {:distributor "Sony/Screen Gems", :genre "Horror", :gross 231550741}
 {:distributor "Fine Line", :genre "Romantic Comedy", :gross 10686841}
 {:distributor "Dreamworks SKG", :genre "Romantic Comedy", :gross 81663263}
 {:distributor "New Line", :genre "Thriller/Suspense", :gross 396547645}
 {:distributor "MGM", :genre "Horror", :gross 600168880}
 {:distributor "Vitagraph Films", :genre "Comedy", :gross 1239183}
 {:distributor "Eros Entertainment", :genre "Comedy", :gross 2217561}
 {:distributor "Vineyard Distribution", :genre "Drama", :gross 2025032}
 {:distributor "Zeitgeist", :genre "Drama", :gross 7607385}
 {:distributor "Yash Raj Films", :genre nil, :gross 164649}
 {:distributor "Sony/TriStar", :genre "Comedy", :gross 41382841}
 {:distributor "Film Movement", :genre nil, :gross 0}
 {:distributor "Slowhand Cinema", :genre "Horror", :gross 0}
 {:distributor "Goldwyn Entertainment", :genre "Action", :gross 10161099}
 {:distributor "Avco Embassy", :genre "Horror", :gross 21378361}
 {:distributor "Paramount Vantage", :genre "Drama", :gross 171994834}
 {:distributor "IFC First Take", :genre "Comedy", :gross 52850}
 {:distributor "Kino International", :genre "Thriller/Suspense", :gross 881950}
 {:distributor "Magnolia Pictures", :genre "Horror", :gross 101740}
 {:distributor "Orion Classics", :genre nil, :gross 0}
 {:distributor "Sony/Columbia", :genre "Drama", :gross 307267103}
 {:distributor "Universal", :genre "Thriller/Suspense", :gross 742615056}
 {:distributor "Magnolia Pictures", :genre "Comedy", :gross 1312043}
 {:distributor "Tartan Films", :genre "Thriller/Suspense", :gross 211667}
 {:distributor "Warner Bros.", :genre "Musical", :gross 124064015}
 {:distributor "USA Films", :genre "Horror", :gross 39235088}
 {:distributor "Freestyle Releasing", :genre "Action", :gross 10977721}
 {:distributor "Women Make Movies", :genre "Documentary", :gross 33312}
 {:distributor "Imaginasian", :genre "Drama", :gross 635305}
 {:distributor "Sony/Columbia", :genre "Western", :gross 33200000}
 {:distributor "IDP/Goldwyn/Roadside", :genre "Drama", :gross 17683184}
 {:distributor "Miramax/Dimension", :genre "Thriller/Suspense", :gross 4476235}
 {:distributor "Wingate Distribution", :genre "Romantic Comedy", :gross 3127472}
 {:distributor "Arab Film Distribution", :genre "Documentary", :gross 6268}
 {:distributor "Focus Features", :genre "Musical", :gross 3076425}
 {:distributor "Eros Entertainment", :genre "Thriller/Suspense", :gross 49000}
 {:distributor "New Line", :genre "Documentary", :gross 3816594}
 {:distributor "Universal", :genre nil, :gross 346146173}
 {:distributor "JeTi Films", :genre "Thriller/Suspense", :gross 527}
 {:distributor "Sony Pictures Classics", :genre "Action", :gross 34721536}
 {:distributor "Fox Searchlight", :genre "Thriller/Suspense", :gross 42232662}
 {:distributor "Paramount Pictures", :genre "Comedy", :gross 3439991383}
 {:distributor "UTV Communications", :genre "Drama", :gross 2197694}
 {:distributor "Sony Pictures", :genre "Black Comedy", :gross 71329714}
 {:distributor "Lionsgate", :genre "Romantic Comedy", :gross 36086615}
 {:distributor "Sony Pictures", :genre "Horror", :gross 866742797}
 {:distributor "Fox Meadow", :genre "Comedy", :gross 8158}
 {:distributor "Destination Films", :genre "Drama", :gross 3134509}
 {:distributor "Vivendi Entertainment", :genre "Drama", :gross 2848587}
 {:distributor "First Look", :genre "Drama", :gross 6559486}
 {:distributor nil, :genre "Thriller/Suspense", :gross 24047701}
 {:distributor "20th Century Fox", :genre "Action", :gross 4472245650}
 {:distributor "Magnolia Pictures", :genre "Thriller/Suspense", :gross 22974}
 {:distributor "Empire Pictures", :genre "Horror", :gross 354704}
 {:distributor "Savoy", :genre "Horror", :gross 11784569}
 {:distributor "Good Machine Releasing", :genre "Comedy", :gross 2746453}
 {:distributor "Sony Pictures", :genre "Thriller/Suspense", :gross 1555978444}
 {:distributor "Cinema Con Sabor", :genre "Drama", :gross 1987}
 {:distributor "TLA Releasing", :genre "Comedy", :gross 5361}
 {:distributor "Fox Searchlight", :genre "Drama", :gross 584711172}
 {:distributor "Walt Disney Pictures", :genre nil, :gross 40565276}
 {:distributor "USA Films", :genre "Black Comedy", :gross 48028980}
 {:distributor "Lionsgate", :genre "Horror", :gross 562700767}
 {:distributor "Miramax", :genre "Thriller/Suspense", :gross 17682823}
 {:distributor "Artisan", :genre "Comedy", :gross 47262852}
 {:distributor "Freestyle Releasing", :genre "Comedy", :gross 614084}
 {:distributor "Sony Pictures", :genre "Comedy", :gross 4303529227}
 {:distributor "Lionsgate", :genre "Action", :gross 375753215}
 {:distributor nil, :genre "Musical", :gross 189300000}
 {:distributor "Warner Bros.", :genre "Romantic Comedy", :gross 519820728}
 {:distributor "Warner Bros.", :genre "Horror", :gross 1201858232}
 {:distributor "Dreamworks SKG", :genre "Thriller/Suspense", :gross 236446080}
 {:distributor "Weinstein Co.", :genre "Comedy", :gross 93875742}
 {:distributor "Walt Disney Pictures", :genre "Action", :gross 1351228825}
 {:distributor "Walt Disney Pictures", :genre "Romantic Comedy", :gross 714774275}
 {:distributor "Orion Pictures", :genre "Black Comedy", :gross 3602884}
 {:distributor "Universal", :genre "Romantic Comedy", :gross 960613297}
 {:distributor "JeTi Films", :genre "Action", :gross 1336}
 {:distributor "ThinkFilm", :genre nil, :gross 7000000}
 {:distributor "Sony/Screen Gems", :genre "Comedy", :gross 21957147}
 {:distributor "Stratosphere Entertainment", :genre nil, :gross 22434}
 {:distributor "Paramount Pictures", :genre "Action", :gross 4057307597}
 {:distributor "United Artists", :genre "Drama", :gross 53067867}
 {:distributor "Focus Features", :genre "Documentary", :gross 11718595}
 {:distributor "Fabrication Films", :genre "Drama", :gross 4134}
 {:distributor "Video Sound", :genre "Drama", :gross 623791}
 {:distributor nil, :genre "Comedy", :gross 84419422}
 {:distributor "Dreamworks SKG", :genre "Action", :gross 294646820}
 {:distributor "Goldwyn Entertainment", :genre nil, :gross 99147}
 {:distributor "Universal", :genre "Comedy", :gross 4822570713}
 {:distributor "Samuel Goldwyn Films", :genre "Musical", :gross 275380}
 {:distributor "Captured Light", :genre "Documentary", :gross 10941801}
 {:distributor "USA Films", :genre "Western", :gross 630779}
 {:distributor "Trimark", :genre "Drama", :gross 19962339}
 {:distributor "Freestyle Releasing", :genre "Drama", :gross 2374190}
 {:distributor "Warner Bros.", :genre "Comedy", :gross 3780632884}
 {:distributor "Universal", :genre "Drama", :gross 2952035381}
 {:distributor "Summit Entertainment", :genre "Horror", :gross 11965282}
 {:distributor "Strand", :genre "Comedy", :gross 26778}
 {:distributor "Roadside Attractions", :genre "Romantic Comedy", :gross 1060591}
 {:distributor "Strand", :genre "Drama", :gross 392465}
 {:distributor "IFC Films", :genre "Romantic Comedy", :gross 241438208}
 {:distributor "Halestorm Entertainment", :genre "Comedy", :gross 1250798}
 {:distributor "Yash Raj Films", :genre "Comedy", :gross 872643}
 {:distributor "Film Sales Company", :genre "Drama", :gross 200433}
 {:distributor "Miramax", :genre "Adventure", :gross 6725194}
 {:distributor "Universal", :genre "Adventure", :gross 1663863871}
 {:distributor "Moviestore Entertainment", :genre "Horror", :gross 1355728}
 {:distributor "New Line", :genre "Action", :gross 1181888113}
 {:distributor "Roxie Releasing", :genre "Drama", :gross 779137}
 {:distributor "Artisan", :genre "Drama", :gross 9682319}
 {:distributor "Excel Entertainment", :genre "Drama", :gross 12830880}
 {:distributor "USA Films", :genre "Drama", :gross 124107476}
 {:distributor "Artisan", :genre "Horror", :gross 180260417}
 {:distributor "United Artists", :genre "Horror", :gross 33244684}
 {:distributor "Summit Entertainment", :genre "Comedy", :gross 26032950}
 {:distributor "MGM", :genre "Drama", :gross 814686477}
 {:distributor "Lionsgate", :genre "Western", :gross 53606916}
 {:distributor "Savoy", :genre "Drama", :gross 25842377}
 {:distributor "MGM/UA Classics", :genre "Musical", :gross 39012241}
 {:distributor "Paramount Pictures", :genre "Adventure", :gross 4452930853}
 {:distributor "Alliance", :genre "Drama", :gross 3623246}
 {:distributor "Anchor Bay Entertainment", :genre nil, :gross 7294}
 {:distributor "United Artists", :genre "Action", :gross 45640882}
 {:distributor "IDP Distribution", :genre "Comedy", :gross 223878}
 {:distributor "Fine Line", :genre "Drama", :gross 55480223}
 {:distributor "Warner Bros.", :genre "Action", :gross 5211153442}
 {:distributor "Cinema Service", :genre "Action", :gross 298347}
 {:distributor "Weinstein Co.", :genre "Drama", :gross 100848443}
 {:distributor "Kino International", :genre "Documentary", :gross 16892}
 {:distributor "Focus Features", :genre "Adventure", :gross 107036123}
 {:distributor "Artisan", :genre "Thriller/Suspense", :gross 39786833}
 {:distributor "Link Productions Ltd.", :genre "Comedy", :gross 1470856}
 {:distributor "Gramercy", :genre nil, :gross 289356}
 {:distributor "Miramax", :genre "Musical", :gross 191609444}
 {:distributor "Roadside Attractions", :genre nil, :gross 1475746}
 {:distributor "Lionsgate", :genre "Documentary", :gross 159942801}
 {:distributor "MGM", :genre "Adventure", :gross 178985491}
 {:distributor "Fine Line", :genre nil, :gross 413802}
 {:distributor "Indican Pictures", :genre "Musical", :gross 12604}
 {:distributor "Paramount Pictures", :genre nil, :gross 249144054}
 {:distributor "New Line", :genre "Musical", :gross 118823091}
 {:distributor "MGM/UA Classics", :genre "Comedy", :gross 13075390}
 {:distributor "Oscilloscope Pictures", :genre nil, :gross 25572}
 {:distributor "Dreamworks SKG", :genre "Drama", :gross 816054060}
 {:distributor "Warner Independent", :genre "Drama", :gross 47534874}
 {:distributor "Borotoro", :genre "Comedy", :gross 551002}
 {:distributor "Goldwyn Entertainment", :genre "Comedy", :gross 2794056}
 {:distributor "United Artists", :genre "Western", :gross 24300000}
 {:distributor "Illuminare", :genre "Drama", :gross 87264}
 {:distributor "Savoy", :genre "Comedy", :gross 7881335}
 {:distributor "IDP/Goldwyn/Roadside", :genre "Musical", :gross 349132}
 {:distributor "Halestone", :genre "Comedy", :gross 1111615}
 {:distributor "Samuel Goldwyn Films", :genre "Thriller/Suspense", :gross 1818681}
 {:distributor "Embassy", :genre "Western", :gross 909000}
 {:distributor "Mela Films", :genre "Documentary", :gross 381225}
 {:distributor "New Line", :genre "Comedy", :gross 1572110783}
 {:distributor "The Bigger Picture", :genre "Thriller/Suspense", :gross 1008849}
 {:distributor "DEJ Productions", :genre "Documentary", :gross 181041}
 {:distributor "Warner Independent", :genre "Thriller/Suspense", :gross 11802747}
 {:distributor "Warner Independent", :genre "Comedy", :gross 5549923}
 {:distributor "Walt Disney Pictures", :genre "Thriller/Suspense", :gross 1018339953}
 {:distributor "New Line", :genre nil, :gross 1192918}
 {:distributor "ThinkFilm", :genre "Drama", :gross 7677791}
 {:distributor "Warner Bros.", :genre "Drama", :gross 3462117014}
 {:distributor "Fox Searchlight", :genre "Comedy", :gross 450769345}
 {:distributor "Dreamworks SKG", :genre nil, :gross 75078}
 {:distributor "Kino International", :genre "Comedy", :gross 163245}
 {:distributor "Rainforest Productions", :genre "Thriller/Suspense", :gross 1161843}
 {:distributor "MGM", :genre "Romantic Comedy", :gross 48206161}
 {:distributor "United Artists", :genre "Adventure", :gross 42000000}
 {:distributor "Hyde Park Films", :genre "Drama", :gross 493296}
 {:distributor "Newmarket Films", :genre "Drama", :gross 438169372}
 {:distributor "Fine Line", :genre "Black Comedy", :gross 28107437}
 {:distributor "Distributor Unknown", :genre "Drama", :gross 1660865}
 {:distributor "New World", :genre "Horror", :gross 33264000}
 {:distributor "First Look", :genre "Comedy", :gross 5520368}
 {:distributor "Paramount Pictures", :genre "Concert/Performance", :gross 57352842}
 {:distributor "Overture Films", :genre "Black Comedy", :gross 32428195}
 {:distributor nil, :genre "Documentary", :gross 261740}
 {:distributor "Lorimar Motion Pictures", :genre "Horror", :gross 9205924}
 {:distributor "ThinkFilm", :genre "Documentary", :gross 2690829}
 {:distributor "Warner Bros.", :genre "Adventure", :gross 4195326029}
 {:distributor "Paramount Vantage", :genre "Documentary", :gross 8117961}
 {:distributor "Fine Line", :genre "Comedy", :gross 12552217}
 {:distributor "MGM", :genre "Comedy", :gross 1278373301}
 {:distributor "Artisan", :genre "Action", :gross 6047856}
 {:distributor "Walt Disney Pictures", :genre "Black Comedy", :gross 97543212}
 {:distributor "Black Diamond Pictures", :genre "Western", :gross 901857}
 {:distributor "Goldwyn Entertainment", :genre "Thriller/Suspense", :gross 5017971}
 {:distributor "Rainbow Releasing", :genre "Comedy", :gross 20008693}
 {:distributor "Gold Circle Films", :genre "Drama", :gross 563711}
 {:distributor "Cannon", :genre "Action", :gross 26339800}
 {:distributor "Warner Bros.", :genre "Western", :gross 107857521}
 {:distributor "Trimark", :genre "Horror", :gross 12969855}
 {:distributor "Gramercy", :genre "Comedy", :gross 33617534}
 {:distributor "New Line", :genre "Romantic Comedy", :gross 54356746}
 {:distributor "TLA Releasing", :genre "Drama", :gross 833118}
 {:distributor "Dreamworks SKG", :genre "Horror", :gross 308242025}
 {:distributor "Sony Pictures", :genre "Romantic Comedy", :gross 1463827116}
 {:distributor "Sky Island", :genre "Romantic Comedy", :gross 44701}
 {:distributor "Wolfe Releasing", :genre "Thriller/Suspense", :gross 0}
 {:distributor "Paramount Pictures", :genre "Thriller/Suspense", :gross 2341436258}
 {:distributor "Fox Searchlight", :genre "Horror", :gross 86843778}
 {:distributor "3D Entertainment", :genre "Documentary", :gross 7714996}
 {:distributor "ThinkFilm", :genre "Comedy", :gross 4185125}
 {:distributor "Destination Films", :genre "Comedy", :gross 19569699}
 {:distributor "Focus Features", :genre "Comedy", :gross 117924345}
 {:distributor "Sony/Screen Gems", :genre "Thriller/Suspense", :gross 195451140}
 {:distributor "Sony Pictures Classics", :genre nil, :gross 5799814}
 {:distributor "Strand", :genre nil, :gross 23390}
 {:distributor "Yash Raj Films", :genre "Drama", :gross 6314333}
 {:distributor "Miramax", :genre "Action", :gross 364411122}
 {:distributor "Big Fat Movies", :genre "Comedy", :gross 4655}
 {:distributor "Roadside Attractions", :genre "Comedy", :gross 347578}
 {:distributor "Overture Films", :genre "Horror", :gross 49454442}
 {:distributor "Orion Pictures", :genre "Action", :gross 161585454}
 {:distributor "Reliance Big Pictures", :genre nil, :gross 199228}
 {:distributor "New Line", :genre "Black Comedy", :gross 2104439}
 {:distributor "Triumph Releasing", :genre "Action", :gross 4372561}
 {:distributor "Sony Pictures", :genre "Documentary", :gross 2276368}
 {:distributor "Kino International", :genre "Drama", :gross 776051}
 {:distributor "Palm Pictures", :genre "Drama", :gross 20918377}
 {:distributor "Walt Disney Pictures", :genre "Horror", :gross 23086480}
 {:distributor "20th Century Fox", :genre "Musical", :gross 428076019}
 {:distributor "20th Century Fox", :genre "Thriller/Suspense", :gross 756556430}
 {:distributor "IFC Films", :genre "Black Comedy", :gross 2426851}
 {:distributor "Walt Disney Pictures", :genre "Comedy", :gross 4325025512}
 {:distributor "Eros Entertainment", :genre "Adventure", :gross 655538}
 {:distributor "Sony Pictures Classics", :genre "Documentary", :gross 2512679}
 {:distributor "Paramount Vantage", :genre "Black Comedy", :gross 4859475}
 {:distributor "Sony Pictures", :genre "Drama", :gross 2063399773}
 {:distributor "Magnolia Pictures", :genre "Documentary", :gross 1433319}
 {:distributor "Walt Disney Pictures", :genre "Drama", :gross 1818923292}
 {:distributor "Summit Entertainment", :genre "Adventure", :gross 34095010}
 {:distributor "Samuel Goldwyn Films", :genre "Drama", :gross 36390422}
 {:distributor "Focus/Rogue Pictures", :genre "Action", :gross 44578516}
 {:distributor "Independent Artists", :genre "Comedy", :gross 2793776}
 {:distributor "October Films", :genre "Comedy", :gross 23112467}
 {:distributor "The Movie Partners", :genre "Thriller/Suspense", :gross 23616}
 {:distributor "Miramax", :genre nil, :gross 28001663}
 {:distributor "Eros Entertainment", :genre "Romantic Comedy", :gross 2109842}
 {:distributor "First Look", :genre "Thriller/Suspense", :gross 19875}
 {:distributor "New Line", :genre "Horror", :gross 645199724}
 {:distributor "Gramercy", :genre "Drama", :gross 110113645}
 {:distributor "20th Century Fox", :genre "Comedy", :gross 4175360381}
 {:distributor "Paramount Vantage", :genre "Comedy", :gross 19310640}
 {:distributor "Paramount Pictures", :genre "Western", :gross 5321508}
 {:distributor "Fine Line", :genre "Documentary", :gross 7768371}
 {:distributor "United Film Distribution Co.", :genre "Drama", :gross 6965361}
 {:distributor "Indican Pictures", :genre "Horror", :gross 31425}
 {:distributor "United Film Distribution Co.", :genre nil, :gross 1500000}
 {:distributor "Paramount Pictures", :genre "Romantic Comedy", :gross 713762297}
 {:distributor "Well.....FGUCK ME ____Spring", :genre "Documentary", :gross 592014}
 {:distributor "Off-Hollywood Distribution", :genre "Drama", :gross 0}
 {:distributor "Lionsgate", :genre "Comedy", :gross 362328341}
 {:distributor "Lorimar Motion Pictures", :genre "Action", :gross 20257000}
 {:distributor "Regent Releasing", :genre "Action", :gross 884}
 {:distributor "Fader Films", :genre "Drama", :gross 49772}
 {:distributor "Live Entertainment", :genre "Thriller/Suspense", :gross 3221152}
 {:distributor "3D Entertainment", :genre nil, :gross 0}
 {:distributor "Samuel Goldwyn Films", :genre "Comedy", :gross 13588339}
 {:distributor "Five & Two Pictures", :genre "Drama", :gross 1500711}
 {:distributor "Overture Films", :genre "Documentary", :gross 14363397}
 {:distributor "Miramax", :genre "Drama", :gross 1367693143}
 {:distributor "Roadside Attractions", :genre "Drama", :gross 14928520}
 {:distributor "Paramount Vantage", :genre "Thriller/Suspense", :gross 12008642}
 {:distributor "Weinstein Co.", :genre "Action", :gross 169554410}
 {:distributor "Miramax", :genre "Horror", :gross 533523111}
 {:distributor "Warner Bros.", :genre "Thriller/Suspense", :gross 1079077925}
 {:distributor "Picturehouse", :genre "Horror", :gross 37634615}
 {:distributor "Strand", :genre "Thriller/Suspense", :gross 36630}
 {:distributor "Galaxy International Releasing", :genre "Horror", :gross 11642254}
 {:distributor "Sony Pictures Classics", :genre "Black Comedy", :gross 617228}
 {:distributor "MGM", :genre "Action", :gross 1721304227}
 {:distributor "Orion Pictures", :genre "Horror", :gross 23816948}
 {:distributor "Cloud Ten Pictures", :genre "Drama", :gross 4221341}
 {:distributor "Avco Embassy", :genre "Action", :gross 25244700}
 {:distributor "Miramax", :genre "Concert/Performance", :gross 2255000}
 {:distributor "Focus/Rogue Pictures", :genre "Comedy", :gross 13542874}
 {:distributor "Lot 47 Films", :genre "Drama", :gross 1138836}
 {:distributor "Sony/Columbia", :genre "Horror", :gross 15990938}
 {:distributor "Lifesize Entertainment", :genre "Documentary", :gross 0}
 {:distributor "Destination Films", :genre "Thriller/Suspense", :gross 16500786}
 {:distributor "New World", :genre "Thriller/Suspense", :gross 5269990}
 {:distributor "20th Century Fox", :genre "Adventure", :gross 4387101470}
 {:distributor "MGM", :genre "Musical", :gross 50555510}
 {:distributor "WinStar Cinema", :genre "Drama", :gross 722636}
 {:distributor "Yari Film Group Releasing", :genre "Drama", :gross 39868642}
 {:distributor "Lavender House Films", :genre "Drama", :gross 401}
 {:distributor "Paramount Pictures", :genre "Drama", :gross 2512800197}
 {:distributor "Shining Excalibur", :genre "Drama", :gross 7412216}
 {:distributor "Sony/Columbia", :genre "Thriller/Suspense", :gross 8801940}
 {:distributor "Fox Searchlight", :genre "Black Comedy", :gross 24793509}
 {:distributor "Indican Pictures", :genre nil, :gross 117099}
 {:distributor nil, :genre "Drama", :gross 30845376}
 {:distributor "Miramax", :genre "Comedy", :gross 211571994}
 {:distributor "Screen Media Films", :genre "Action", :gross 13638608}
 {:distributor "Lionsgate", :genre "Thriller/Suspense", :gross 176788980}
 {:distributor "RKO Radio Pictures", :genre "Musical", :gross 0}
 {:distributor "Avatar", :genre nil, :gross 1358}
 {:distributor "Orion Pictures", :genre "Western", :gross 184208842}
 {:distributor "Eros Entertainment", :genre nil, :gross 1183658}
 {:distributor "RS Entertainment", :genre "Drama", :gross 5781086}
 {:distributor "IFC Films", :genre "Comedy", :gross 154187}
 {:distributor "Artisan", :genre "Black Comedy", :gross 1276984}
 {:distributor "Hemdale", :genre "Drama", :gross 1500000}
 {:distributor "Apparition", :genre "Drama", :gross 15445909}
 {:distributor "Fox Searchlight", :genre "Romantic Comedy", :gross 60981802}
 {:distributor "October Films", :genre nil, :gross 21210}
 {:distributor "Indican Pictures", :genre "Action", :gross 30471}
 {:distributor "USA Films", :genre "Musical", :gross 6201757}
 {:distributor "20th Century Fox", :genre "Horror", :gross 345544922}
 {:distributor "Summit Entertainment", :genre "Thriller/Suspense", :gross 127310710}
 {:distributor "October Films", :genre "Drama", :gross 35363576}
 {:distributor "Walt Disney Pictures", :genre "Musical", :gross 649800217}
 {:distributor "Walt Disney Pictures", :genre "Concert/Performance", :gross 65281781}
 {:distributor "Freestyle Releasing", :genre "Horror", :gross 16298469}
 {:distributor "Sony Pictures", :genre "Action", :gross 3617862308}
 {:distributor "Matson", :genre "Drama", :gross 120620}
 {:distributor "Warner Independent", :genre "Documentary", :gross 77437223}
 {:distributor "Fox Searchlight", :genre "Action", :gross 1502188}
 {:distributor "Goldwyn Entertainment", :genre "Drama", :gross 28268338}
 {:distributor "Overture Films", :genre "Comedy", :gross 9427026}
 {:distributor "Dreamworks SKG", :genre "Adventure", :gross 1625558442}
 {:distributor "Orion Pictures", :genre "Thriller/Suspense", :gross 132726716}
 {:distributor "Miramax/Dimension", :genre "Adventure", :gross 387376311}
 {:distributor "RBC Radio", :genre "Drama", :gross 227241}
 {:distributor "Island/Alive", :genre "Drama", :gross 390659}
 {:distributor "United Film Distribution Co.", :genre "Horror", :gross 11159262}
 {:distributor "Universal", :genre "Musical", :gross 89298336}
 {:distributor "Freestyle Releasing", :genre "Adventure", :gross 915840}
 {:distributor "Gramercy", :genre "Thriller/Suspense", :gross 28370011}
 {:distributor "New Century Vista Film Company", :genre "Horror", :gross 1705139}
 {:distributor "AdLab Films", :genre "Action", :gross 1430721}
 {:distributor "Focus/Rogue Pictures", :genre "Romantic Comedy", :gross 10525717}
 {:distributor "Universal", :genre "Horror", :gross 869279889}
 {:distributor "Atlantic", :genre "Comedy", :gross 7888000}
 {:distributor "Cowboy Pictures", :genre "Action", :gross 271736}
 {:distributor "Alliance", :genre "Adventure", :gross 999811}
 {:distributor "Miramax", :genre "Western", :gross 16150499}
 {:distributor "Sony Pictures", :genre "Adventure", :gross 2637303280}
 {:distributor "Destination Films", :genre "Adventure", :gross 15911332}
 {:distributor "Paramount Pictures", :genre "Musical", :gross 215349994}
 {:distributor "20th Century Fox", :genre "Drama", :gross 1342488595}
 {:distributor "Cinema Guild", :genre "Horror", :gross 7369373}
 {:distributor "IDP/Goldwyn/Roadside", :genre nil, :gross 429273}
 {:distributor "IFC Films", :genre "Thriller/Suspense", :gross 174492}
 {:distributor nil, :genre "Romantic Comedy", :gross 2807631}
 {:distributor "United Artists", :genre "Romantic Comedy", :gross 18600000}
 {:distributor "Island", :genre "Comedy", :gross 7137502}
 {:distributor "First Look", :genre "Black Comedy", :gross 580862}
 {:distributor "Avco Embassy", :genre "Adventure", :gross 37400000}
 {:distributor "Sony/Screen Gems", :genre "Drama", :gross 190492997}
 {:distributor "Vitagraph Films", :genre "Drama", :gross 75828}
 {:distributor "RKO Radio Pictures", :genre "Adventure", :gross 87400000}
 {:distributor "Sony/Columbia", :genre "Action", :gross 61537744}
 {:distributor "Phaedra Cinema", :genre nil, :gross 195043}
 {:distributor "Trimark", :genre "Comedy", :gross 5020805}
 {:distributor "Paramount Pictures", :genre "Black Comedy", :gross 46648740}
 {:distributor "Sony/TriStar", :genre "Drama", :gross 170533004}
 {:distributor "Weinstein/Dimension", :genre "Action", :gross 6412374}
 {:distributor "United Film Distribution Co.", :genre "Comedy", :gross 15000000}
 {:distributor "Sony/Screen Gems", :genre "Action", :gross 131489618}
 {:distributor "Miramax/Dimension", :genre "Romantic Comedy", :gross 15549702}
 {:distributor "MGM", :genre "Documentary", :gross 21576018}
 {:distributor "Magnolia Pictures", :genre "Action", :gross 4183601}
 {:distributor "Sony Pictures", :genre "Western", :gross 45467669}
 {:distributor "Vivendi Entertainment", :genre "Comedy", :gross 7013191}
 {:distributor "Summit Entertainment", :genre "Drama", :gross 876349628}
 {:distributor "Miramax/Dimension", :genre "Horror", :gross 111169543}
 {:distributor "IFC Films", :genre "Drama", :gross 20526902}
 {:distributor "New Yorker", :genre "Comedy", :gross 245359}
 {:distributor "Universal", :genre "Black Comedy", :gross 17243162}
 {:distributor "M Power Releasing", :genre "Drama", :gross 11748661}
 {:distributor "Castle Hill Productions", :genre "Drama", :gross 0}
 {:distributor "Magnolia Pictures", :genre nil, :gross 253032}
 {:distributor "Waterdog Films", :genre "Romantic Comedy", :gross 10744}
 {:distributor "Warner Bros.", :genre "Black Comedy", :gross 18869794}
 {:distributor "WellSpring", :genre "Drama", :gross 1619231}
 {:distributor "Walter Reade Organization", :genre "Horror", :gross 12000000}
 {:distributor "Warner Independent", :genre "Action", :gross 669625}
 {:distributor "Sony/Columbia", :genre "Adventure", :gross 25349444}
 {:distributor "Overture Films", :genre "Thriller/Suspense", :gross 164133561}
 {:distributor "Hombre de Oro", :genre "Drama", :gross 886410}
 {:distributor "CBS Films", :genre "Drama", :gross 12482741}
 {:distributor "Romar", :genre "Comedy", :gross 3700}
 {:distributor "Lionsgate", :genre "Adventure", :gross 10115431}
 {:distributor "Picturehouse", :genre "Comedy", :gross 28155488}
    {:distributor "Miramax", :genre "Black Comedy", :gross 11357579}))

(def data2
  '({:gene "WNT2", :sample "508", :value "4.69455368790816"}
 {:gene "WNT2", :sample "520", :value "4.28285130191303"}
 {:gene "WNT2", :sample "521", :value "1.2370000288886"}
 {:gene "WNT2", :sample "512", :value "5.98372036486048"}
 {:gene "WNT2", :sample "509", :value "1.3328580234688"}
 {:gene "WNT2", :sample "517", :value "-0.165525201539909"}
 {:gene "WNT2", :sample "516", :value "2.11057837942801"}
 {:gene "WNT2", :sample "513", :value "2.89864835625364"}
 {:gene "DNM1", :sample "508", :value "6.18073526217034"}
 {:gene "DNM1", :sample "520", :value "6.28530033555224"}
 {:gene "DNM1", :sample "521", :value "4.48895537322577"}
 {:gene "DNM1", :sample "512", :value "5.66002401990528"}
 {:gene "DNM1", :sample "509", :value "4.44196514365529"}
 {:gene "DNM1", :sample "517", :value "3.9603755075091"}
 {:gene "DNM1", :sample "516", :value "5.8002922874965"}
 {:gene "DNM1", :sample "513", :value "3.98151281042034"}
 {:gene "ZBTB16", :sample "508", :value "-1.86352251820081"}
 {:gene "ZBTB16", :sample "520", :value "-0.515040727317009"}
 {:gene "ZBTB16", :sample "521", :value "5.7791730037569"}
 {:gene "ZBTB16", :sample "512", :value "-1.7771519191856"}
 {:gene "ZBTB16", :sample "509", :value "5.25796696285121"}
 {:gene "ZBTB16", :sample "517", :value "4.28308662399116"}
 {:gene "ZBTB16", :sample "516", :value "-2.93197221657965"}
 {:gene "ZBTB16", :sample "513", :value "4.9022228878453"}
 {:gene "DUSP1", :sample "508", :value "4.93655107030217"}
 {:gene "DUSP1", :sample "520", :value "5.14324586287326"}
 {:gene "DUSP1", :sample "521", :value "8.39670934427623"}
 {:gene "DUSP1", :sample "512", :value "5.60756813037785"}
 {:gene "DUSP1", :sample "509", :value "8.01907429882787"}
 {:gene "DUSP1", :sample "517", :value "7.72298976466619"}
 {:gene "DUSP1", :sample "516", :value "5.02834170722824"}
 {:gene "DUSP1", :sample "513", :value "8.30219557783798"}
 {:gene "HIF3A", :sample "508", :value "1.0139912580994"}
 {:gene "HIF3A", :sample "520", :value "2.25012442319609"}
 {:gene "HIF3A", :sample "521", :value "4.79058092806672"}
 {:gene "HIF3A", :sample "512", :value "1.57525031122943"}
 {:gene "HIF3A", :sample "509", :value "3.37445673080234"}
 {:gene "HIF3A", :sample "517", :value "2.83358793070328"}
 {:gene "HIF3A", :sample "516", :value "0.492887784795945"}
 {:gene "HIF3A", :sample "513", :value "4.15474019163534"}
 {:gene "MT2A", :sample "508", :value "6.24851399016412"}
 {:gene "MT2A", :sample "520", :value "5.91636282515681"}
 {:gene "MT2A", :sample "521", :value "7.93349175824132"}
 {:gene "MT2A", :sample "512", :value "5.78285535349592"}
 {:gene "MT2A", :sample "509", :value "8.27698758640279"}
 {:gene "MT2A", :sample "517", :value "8.20158550590639"}
 {:gene "MT2A", :sample "516", :value "5.75838967893522"}
 {:gene "MT2A", :sample "513", :value "8.07084582202555"}
 {:gene "FGD4", :sample "508", :value "4.13241331926396"}
 {:gene "FGD4", :sample "520", :value "4.58981850877196"}
 {:gene "FGD4", :sample "521", :value "7.03908171742351"}
 {:gene "FGD4", :sample "512", :value "4.2162201879756"}
 {:gene "FGD4", :sample "509", :value "6.2395775164949"}
 {:gene "FGD4", :sample "517", :value "6.30382156424754"}
 {:gene "FGD4", :sample "516", :value "4.31271009913433"}
 {:gene "FGD4", :sample "513", :value "6.50205756195231"}
 {:gene "PRSS35", :sample "508", :value "3.93001559453838"}
 {:gene "PRSS35", :sample "520", :value "4.48262215247243"}
 {:gene "PRSS35", :sample "521", :value "1.607251411321"}
 {:gene "PRSS35", :sample "512", :value "5.18907269381948"}
 {:gene "PRSS35", :sample "509", :value "1.14344085971871"}
 {:gene "PRSS35", :sample "517", :value "2.06814189565329"}
 {:gene "PRSS35", :sample "516", :value "4.84830664593644"}
 {:gene "PRSS35", :sample "513", :value "2.64982582436249"}
 {:gene "ADAM12", :sample "508", :value "6.67785831073959"}
 {:gene "ADAM12", :sample "520", :value "5.81732874866786"}
 {:gene "ADAM12", :sample "521", :value "4.14377753052124"}
 {:gene "ADAM12", :sample "512", :value "6.98451044997398"}
 {:gene "ADAM12", :sample "509", :value "4.82250784199589"}
 {:gene "ADAM12", :sample "517", :value "5.10896456918063"}
 {:gene "ADAM12", :sample "516", :value "6.86031250858483"}
 {:gene "ADAM12", :sample "513", :value "4.96455744842029"}
 {:gene "SPARCL1", :sample "508", :value "1.55470970310097"}
 {:gene "SPARCL1", :sample "520", :value "2.03488498060457"}
 {:gene "SPARCL1", :sample "521", :value "6.71983276215129"}
 {:gene "SPARCL1", :sample "512", :value "2.02259555555733"}
 {:gene "SPARCL1", :sample "509", :value "6.72069644167212"}
 {:gene "SPARCL1", :sample "517", :value "6.00586264525223"}
 {:gene "SPARCL1", :sample "516", :value "2.02834001111556"}
 {:gene "SPARCL1", :sample "513", :value "6.34935521091851"}
 {:gene "ACSS1", :sample "508", :value "3.71483726631926"}
 {:gene "ACSS1", :sample "520", :value "4.23313747152727"}
 {:gene "ACSS1", :sample "521", :value "5.99510828523058"}
 {:gene "ACSS1", :sample "512", :value "3.86937676723286"}
 {:gene "ACSS1", :sample "509", :value "5.56550314244328"}
 {:gene "ACSS1", :sample "517", :value "4.88790377026001"}
 {:gene "ACSS1", :sample "516", :value "2.91630176912867"}
 {:gene "ACSS1", :sample "513", :value "5.79920433839758"}
 {:gene "TIMP4", :sample "508", :value "0.598793673732585"}
 {:gene "TIMP4", :sample "520", :value "0.819552014211164"}
 {:gene "TIMP4", :sample "521", :value "3.63867920029566"}
 {:gene "TIMP4", :sample "512", :value "0.580523267025356"}
 {:gene "TIMP4", :sample "509", :value "3.76820277073184"}
 {:gene "TIMP4", :sample "517", :value "4.25311956115208"}
 {:gene "TIMP4", :sample "516", :value "1.5275170040646"}
 {:gene "TIMP4", :sample "513", :value "3.37516813739973"}
 {:gene "STEAP2", :sample "508", :value "5.90927303326086"}
 {:gene "STEAP2", :sample "520", :value "5.94039233384028"}
 {:gene "STEAP2", :sample "521", :value "7.93029052160909"}
 {:gene "STEAP2", :sample "512", :value "6.0463676874266"}
 {:gene "STEAP2", :sample "509", :value "7.72325041735153"}
 {:gene "STEAP2", :sample "517", :value "7.2693879518966"}
 {:gene "STEAP2", :sample "516", :value "5.34380759907602"}
 {:gene "STEAP2", :sample "513", :value "8.15485426186916"}
 {:gene "PDPN", :sample "508", :value "5.35753249909343"}
 {:gene "PDPN", :sample "520", :value "4.18969108667715"}
 {:gene "PDPN", :sample "521", :value "5.99287820478704"}
 {:gene "PDPN", :sample "512", :value "4.35562719096266"}
 {:gene "PDPN", :sample "509", :value "7.20677629425236"}
 {:gene "PDPN", :sample "517", :value "5.02574217372951"}
 {:gene "PDPN", :sample "516", :value "3.12195409198714"}
 {:gene "PDPN", :sample "513", :value "6.28395625499249"}
 {:gene "NEXN", :sample "508", :value "6.38633543860892"}
 {:gene "NEXN", :sample "520", :value "6.85334055833747"}
 {:gene "NEXN", :sample "521", :value "8.71889675737361"}
 {:gene "NEXN", :sample "512", :value "6.79475336433913"}
 {:gene "NEXN", :sample "509", :value "8.60998436036047"}
 {:gene "NEXN", :sample "517", :value "8.42466113213688"}
 {:gene "NEXN", :sample "516", :value "6.47359951916285"}
 {:gene "NEXN", :sample "513", :value "8.8347306816487"}
 {:gene "DNAJB4", :sample "508", :value "5.03276842874731"}
 {:gene "DNAJB4", :sample "520", :value "5.06014990916259"}
 {:gene "DNAJB4", :sample "521", :value "6.54135805502078"}
 {:gene "DNAJB4", :sample "512", :value "5.2842361428814"}
 {:gene "DNAJB4", :sample "509", :value "6.5087197277283"}
 {:gene "DNAJB4", :sample "517", :value "6.70020334810133"}
 {:gene "DNAJB4", :sample "516", :value "5.2410588772593"}
 {:gene "DNAJB4", :sample "513", :value "6.87435015921954"}
 {:gene "VCAM1", :sample "508", :value "5.39422051601125"}
 {:gene "VCAM1", :sample "520", :value "5.03804927591473"}
 {:gene "VCAM1", :sample "521", :value "0.694381107656567"}
 {:gene "VCAM1", :sample "512", :value "5.58242033138669"}
 {:gene "VCAM1", :sample "509", :value "1.71977948787564"}
 {:gene "VCAM1", :sample "517", :value "2.53465016621424"}
 {:gene "VCAM1", :sample "516", :value "5.76263513289002"}
 {:gene "VCAM1", :sample "513", :value "1.96834777623529"}
 {:gene "CACNB2", :sample "508", :value "1.91233417188757"}
 {:gene "CACNB2", :sample "520", :value "2.31037269750291"}
 {:gene "CACNB2", :sample "521", :value "5.60468736900362"}
 {:gene "CACNB2", :sample "512", :value "2.58230398391373"}
 {:gene "CACNB2", :sample "509", :value "5.24230161509031"}
 {:gene "CACNB2", :sample "517", :value "4.58122522039289"}
 {:gene "CACNB2", :sample "516", :value "1.48703785665958"}
 {:gene "CACNB2", :sample "513", :value "5.83652615191799"}
 {:gene "FAM107A", :sample "508", :value "-0.639114866992957"}
 {:gene "FAM107A", :sample "520", :value "-1.76598130170826"}
 {:gene "FAM107A", :sample "521", :value "3.26276217161494"}
 {:gene "FAM107A", :sample "512", :value "0.580523267025356"}
 {:gene "FAM107A", :sample "509", :value "3.83238908678589"}
 {:gene "FAM107A", :sample "517", :value "1.17570994805675"}
 {:gene "FAM107A", :sample "516", :value "-2.93197221657965"}
 {:gene "FAM107A", :sample "513", :value "4.97415813302148"}
 {:gene "MAOA", :sample "508", :value "4.34615499416104"}
 {:gene "MAOA", :sample "520", :value "3.53657663182848"}
 {:gene "MAOA", :sample "521", :value "7.42383345715469"}
 {:gene "MAOA", :sample "512", :value "4.53361197669738"}
 {:gene "MAOA", :sample "509", :value "7.58181859772461"}
 {:gene "MAOA", :sample "517", :value "7.73478409076481"}
 {:gene "MAOA", :sample "516", :value "4.70786106177427"}
 {:gene "MAOA", :sample "513", :value "7.75164740340675"}))b
