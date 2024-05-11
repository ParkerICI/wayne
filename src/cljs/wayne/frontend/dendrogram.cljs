(ns wayne.frontend.dendrogram
  (:require [way.vega :as v]
            [wayne.heatmap-data :as hd]
            )
  )

;;; Based on https://vega.github.io/vega/examples/tree-layout/
;;; TODO row/col annotations w colors and scales, see examples

(defn tree
  [cluster-data left?]
  (let [width-signal (if left? "dend_width" "hm_width")
        height-signal (if left? "hm_height" "dend_width")]
    `{:type "group"
      :style "cell"
      :data 
      [
       {:name "links",
        :source ~cluster-data,
        :transform
        [{:type "treelinks"}
         {:type "linkpath",
          :orient ~(if left? "horizontal" "vertical")
          :shape "orthogonal"}]}]
      :encoding {:width {:signal ~width-signal}, 
                 :height {:signal ~height-signal}
                 :strokeWidth {:value 0}
                 }
      :marks [{:type "path",
               :from {:data "links"},
               :encode {:enter
                        {:path {:field "path"},
                         :stroke {:value "#666"}}}}

              #_
              {:type "text",
               :from {:data "tree"},
               :encode              
               {:enter
                {:text {:field "id"},
                 :fontSize {:value 9},
                 :baseline {:value "middle"}},
                :update
                {:x {:field "x"},
                 :y {:field "y"},
                 ;; :dy {:signal "datum.children ? -7 : 7"},
                 ;; :align {:signal "datum.children ? 'right' : 'left'"},
                 :angle {:value 90}
                 :tooltip {:signal "datum"}
                 :opacity {:signal "labels ? 1 : 0"}}}}],
      }))


;;; Generates TWO data specs (for tree and filtered to leaves)
(defn tree-data-spec
  [name clusters left?]
  `[{:name ~name,
     :values ~clusters
     :transform
     [{:type "stratify", :key "id", :parentKey "parent"}
      {:type "tree",
       :method "cluster",
       :size [{:signal ~(if left? "hm_height" "hm_width")} ;;  
              {:signal "dend_width"}],
       :as ~(if left?
              ["y" "x" "depth" "children"]
              ["x" "y" "depth" "children"])}]}
    {:name ~(str name "-leaf")
     :source ~name
     :transform [{:type "filter" :expr "datum.children == 0"}]}]
  )


(defn spec
  [data h-field v-field value-field h-clusters v-clusters]
  (let [hsize (count (distinct (map h-field data))) ;wanted to this in vega but circularities are interfering
        vsize (count (distinct (map v-field data)))]
    `{:description "A clustered heatmap with side-dendrograms",
      :$schema "https://vega.github.io/schema/vega/v5.json",
      :layout {:align "each"
               :columns 2}
      :data [{:name "hm",
              :values ~data}
             ~@(tree-data-spec "ltree" h-clusters true)
             ~@(tree-data-spec "utree" v-clusters false)
             ]
      :scales
      ;; Note: min is because sorting apparently requires an aggregation? And there's no pickone
      [{:name "sx" :type "band" :domain  {:data "utree-leaf" :field "id" :sort {:field "x" :op "min"}} :range {:step 20} } 
       {:name "sy" :type "band" :domain  {:data "ltree-leaf" :field "id" :sort {:field "y" :op "min"}} :range {:step 20}} 
       {:name "color"
        :type "linear"
        :range {:scheme "BlueOrange"}
        :domain {:data "hm", :field ~value-field},
        }]
      :signals
      [{:name "labels", :value true, :bind {:input "checkbox"}}
       {:name "hm_width" :value ~(* 20 vsize)} ; :update "length(data(\"utree-leaf\"))" } ;TODO 20 x counts
       {:name "hm_height" :value ~(* 20 hsize)}
       {:name "dend_width" :value 60}]
      :padding 5,
      :marks
      [
       {:type :group                       ;Empty quadrant
        :style :cell
        :encode {:enter {:width {:signal "dend_width"},
                         :height {:signal "dend_width"}
                         :strokeWidth {:value 0}
                         }
                 }
        }

       ;; V tree
       ~(tree "utree" false)

       ;; H tree
       ~(tree "ltree" true)

       ;; actual hmap 
       {:type "group"
        :name "heatmap"
        :style "cell"
        :encode {
                 :update {
                          :width {:signal "hm_width"}
                          :height {:signal "hm_height"}
                          }
                 },

        :axes
        [{:orient :right :scale :sy :title ~h-field } 
         {:orient :bottom :scale :sx :labelAngle 90 :labelAlign "left" :title ~v-field}]

        :legends
        [{:fill :color
          :type :gradient
          ;; :title "Median feature value"
          :titleOrient "bottom"
          :gradientLength {:signal "hm_height / 2"}
          }]

        :marks
        [{:type "rect"
          :from {:data "hm"}
          :encode
          {:enter
           {:y {:field ~h-field :scale "sy"}
            :x {:field ~v-field :scale "sx"}
            :width {:value 19} :height {:value 19}
            :fill {:field ~value-field :scale "color"}
            }}}
         ]
        }
       ]}) )

(def data1
  '({:mean 5.435251712680062E-6, :feature_variable "VISTA", :final_diagnosis "GBM"}
    {:mean 3.569141518783753E-4, :feature_variable "CD47", :final_diagnosis "GBM"}
    {:mean 4.2118552300283286E-4, :feature_variable "Tox", :final_diagnosis "GBM"}
    {:mean 1.6504745474783288E-4, :feature_variable "FoxP3", :final_diagnosis "GBM"}
    {:mean 2.6179286911936817E-4, :feature_variable "PDL1", :final_diagnosis "GBM"}
    {:mean 3.0870654159171013E-4, :feature_variable "H3K27me3", :final_diagnosis "GBM"}
    {:mean 9.390286885757013E-4, :feature_variable "CD123", :final_diagnosis "GBM"}
    {:mean 3.729930679381619E-6, :feature_variable "LAG3", :final_diagnosis "GBM"}
    {:mean 7.42428062203073E-4, :feature_variable "TMEM119", :final_diagnosis "GBM"}
    {:mean 2.079073988228261E-4, :feature_variable "B7H3", :final_diagnosis "GBM"}
    {:mean 2.907877884574783E-4, :feature_variable "CD206", :final_diagnosis "GBM"}
    {:mean 1.487641417859596E-4, :feature_variable "ICOS", :final_diagnosis "GBM"}
    {:mean 1.924492539905969E-4, :feature_variable "IDO1", :final_diagnosis "GBM"}
    {:mean 3.316939610251262E-4, :feature_variable "GFAP", :final_diagnosis "GBM"}
    {:mean 4.874254806750187E-4, :feature_variable "CD163", :final_diagnosis "GBM"}
    {:mean 6.818355723442709E-5, :feature_variable "IDH1_R132H", :final_diagnosis "GBM"}
    {:mean 5.958878428238913E-5, :feature_variable "CD208", :final_diagnosis "GBM"}
    {:mean 8.730316948045017E-4, :feature_variable "CD45", :final_diagnosis "GBM"}
    {:mean 5.888110159940105E-5, :feature_variable "GPC2", :final_diagnosis "GBM"}
    {:mean 1.136951674554856E-4, :feature_variable "Calprotectin", :final_diagnosis "GBM"}
    {:mean 3.755483448638948E-4, :feature_variable "HLADR", :final_diagnosis "GBM"}
    {:mean 0.0014289789710643214, :feature_variable "HLA1", :final_diagnosis "GBM"}
    {:mean 1.247284319039051E-4, :feature_variable "CD40", :final_diagnosis "GBM"}
    {:mean 1.9880449685238217E-4, :feature_variable "GM2_GD2", :final_diagnosis "GBM"}
    {:mean 1.272480825014225E-5, :feature_variable "Chym_Tryp", :final_diagnosis "GBM"}
    {:mean 1.6793249508127928E-4, :feature_variable "CD209", :final_diagnosis "GBM"}
    {:mean 6.146861398241142E-4, :feature_variable "CD68", :final_diagnosis "GBM"}
    {:mean 2.2103916528012975E-4, :feature_variable "EGFR", :final_diagnosis "GBM"}
    {:mean 4.927524210684343E-5, :feature_variable "CD133", :final_diagnosis "GBM"}
    {:mean 7.660678181109992E-6, :feature_variable "PD1", :final_diagnosis "GBM"}
    {:mean 3.015622885218721E-4, :feature_variable "CD8", :final_diagnosis "GBM"}
    {:mean 5.65467822070042E-5, :feature_variable "EphA2", :final_diagnosis "GBM"}
    {:mean 2.1380888147366912E-5, :feature_variable "CD31", :final_diagnosis "GBM"}
    {:mean 1.7442172656993295E-4, :feature_variable "GLUT1", :final_diagnosis "GBM"}
    {:mean 1.328630396537161E-4, :feature_variable "CD4", :final_diagnosis "GBM"}
    {:mean 3.3802536497788916E-5, :feature_variable "NG2", :final_diagnosis "GBM"}
    {:mean 1.9026522013292543E-4, :feature_variable "ApoE", :final_diagnosis "GBM"}
    {:mean 3.579819819707625E-5, :feature_variable "HER2", :final_diagnosis "GBM"}
    {:mean 1.4287429226248773E-4, :feature_variable "CD3", :final_diagnosis "GBM"}
    {:mean 4.541873717523105E-4, :feature_variable "CD11b", :final_diagnosis "GBM"}
    {:mean 0.0010454574573595644, :feature_variable "Ki67", :final_diagnosis "GBM"}
    {:mean 5.512683873876278E-5, :feature_variable "Olig2", :final_diagnosis "GBM"}
    {:mean 3.244236758513358E-5, :feature_variable "EGFRvIII", :final_diagnosis "GBM"}
    {:mean 1.319912711019263E-4, :feature_variable "CD141", :final_diagnosis "GBM"}
    {:mean 1.7400175770743032E-5, :feature_variable "H3K27M", :final_diagnosis "GBM"}
    {:mean 9.453209327788337E-4, :feature_variable "CD14", :final_diagnosis "GBM"}
    {:mean 2.617491767573066E-4, :feature_variable "CD38", :final_diagnosis "GBM"}
    {:mean 4.2948244449216667E-4, :feature_variable "NeuN", :final_diagnosis "GBM"}
    {:mean 6.157040419903136E-4, :feature_variable "TIM3", :final_diagnosis "GBM"}
    {:mean 1.5681305144397772E-4, :feature_variable "CD86", :final_diagnosis "GBM"}
    {:mean 2.605118797890185E-6, :feature_variable "PD1", :final_diagnosis "Astrocytoma"}
    {:mean 1.443555743043118E-4, :feature_variable "CD209", :final_diagnosis "Astrocytoma"}
    {:mean 7.330509254214242E-4, :feature_variable "TMEM119", :final_diagnosis "Astrocytoma"}
    {:mean 3.091440465510177E-5, :feature_variable "CD133", :final_diagnosis "Astrocytoma"}
    {:mean 1.5883893134051186E-4, :feature_variable "B7H3", :final_diagnosis "Astrocytoma"}
    {:mean 2.753034791484547E-4, :feature_variable "CD8", :final_diagnosis "Astrocytoma"}
    {:mean 1.168462592774157E-4, :feature_variable "CD40", :final_diagnosis "Astrocytoma"}
    {:mean 1.1951572895344921E-4, :feature_variable "Calprotectin", :final_diagnosis "Astrocytoma"}
    {:mean 3.0334026123417717E-4, :feature_variable "ApoE", :final_diagnosis "Astrocytoma"}
    {:mean 3.802224675627309E-4, :feature_variable "CD163", :final_diagnosis "Astrocytoma"}
    {:mean 3.5346674482734804E-5, :feature_variable "NG2", :final_diagnosis "Astrocytoma"}
    {:mean 1.1366508486191059E-4, :feature_variable "CD208", :final_diagnosis "Astrocytoma"}
    {:mean 3.554174493267095E-5, :feature_variable "H3K27M", :final_diagnosis "Astrocytoma"}
    {:mean 2.8143297224757246E-4, :feature_variable "CD206", :final_diagnosis "Astrocytoma"}
    {:mean 9.436492969333805E-5, :feature_variable "CD3", :final_diagnosis "Astrocytoma"}
    {:mean 4.938382783520005E-5, :feature_variable "GPC2", :final_diagnosis "Astrocytoma"}
    {:mean 8.071504286772391E-5, :feature_variable "FoxP3", :final_diagnosis "Astrocytoma"}
    {:mean 1.1674870014049948E-4, :feature_variable "IDO1", :final_diagnosis "Astrocytoma"}
    {:mean 3.986846359836467E-4, :feature_variable "CD11b", :final_diagnosis "Astrocytoma"}
    {:mean 6.929385191399755E-5, :feature_variable "CD4", :final_diagnosis "Astrocytoma"}
    {:mean 6.333813118196492E-5, :feature_variable "EphA2", :final_diagnosis "Astrocytoma"}
    {:mean 1.0419491847630113E-4, :feature_variable "EGFR", :final_diagnosis "Astrocytoma"}
    {:mean 8.958427423533979E-4, :feature_variable "CD123", :final_diagnosis "Astrocytoma"}
    {:mean 2.5943151418696467E-4, :feature_variable "GLUT1", :final_diagnosis "Astrocytoma"}
    {:mean 4.134781846638654E-5, :feature_variable "HER2", :final_diagnosis "Astrocytoma"}
    {:mean 5.274036070004997E-4, :feature_variable "CD38", :final_diagnosis "Astrocytoma"}
    {:mean 1.1519436211982258E-4, :feature_variable "CD86", :final_diagnosis "Astrocytoma"}
    {:mean 3.272369918976462E-5, :feature_variable "EGFRvIII", :final_diagnosis "Astrocytoma"}
    {:mean 1.0174066638959186E-4, :feature_variable "CD86", :final_diagnosis "PXA"}
    {:mean 6.302289409336178E-4, :feature_variable "PDL1", :final_diagnosis "PXA"}
    {:mean 1.8891638867569747E-4, :feature_variable "B7H3", :final_diagnosis "PXA"}
    {:mean 6.847035544945747E-4, :feature_variable "Ki67", :final_diagnosis "PXA"}
    {:mean 7.018865352989501E-5, :feature_variable "GM2_GD2", :final_diagnosis "PXA"}
    {:mean 1.8660495133105212E-4, :feature_variable "CD38", :final_diagnosis "PXA"}
    {:mean 3.880639589840673E-4, :feature_variable "TIM3", :final_diagnosis "PXA"}
    {:mean 2.19382906311942E-4, :feature_variable "GFAP", :final_diagnosis "PXA"}
    {:mean 1.8794857386836652E-4, :feature_variable "CD206", :final_diagnosis "PXA"}
    {:mean 3.401819480960244E-4, :feature_variable "Tox", :final_diagnosis "PXA"}
    {:mean 2.7876517541993594E-5, :feature_variable "Olig2", :final_diagnosis "PXA"}
    {:mean 5.223684938337125E-6, :feature_variable "VISTA", :final_diagnosis "PXA"}
    {:mean 8.145447027497019E-4, :feature_variable "CD47", :final_diagnosis "PXA"}
    {:mean 1.4026032048564468E-4, :feature_variable "GLUT1", :final_diagnosis "PXA"}
    {:mean 5.210741125211914E-6, :feature_variable "Chym_Tryp", :final_diagnosis "PXA"}
    {:mean 1.234667749357791E-5, :feature_variable "CD133", :final_diagnosis "PXA"}
    {:mean 3.197357817614069E-5, :feature_variable "HER2", :final_diagnosis "PXA"}
    {:mean 5.339886681909386E-5, :feature_variable "IDH1_R132H", :final_diagnosis "PXA"}
    {:mean 1.9395771126187498E-5, :feature_variable "EGFR", :final_diagnosis "PXA"}
    {:mean 4.117515812924931E-4, :feature_variable "CD11b", :final_diagnosis "PXA"}
    {:mean 3.6276717546291493E-4, :feature_variable "CD123", :final_diagnosis "PXA"}
    {:mean 1.3482807778454744E-4, :feature_variable "ApoE", :final_diagnosis "PXA"}
    {:mean 1.0561127722554538E-4, :feature_variable "Calprotectin", :final_diagnosis "PXA"}
    {:mean 6.124381344017648E-4, :feature_variable "HLADR", :final_diagnosis "PXA"}
    {:mean 2.4304175700419505E-4, :feature_variable "CD8", :final_diagnosis "PXA"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "PXA"}
    {:mean 8.043508178130612E-4, :feature_variable "CD45", :final_diagnosis "PXA"}
    {:mean 8.218683788550309E-4, :feature_variable "CD14", :final_diagnosis "PXA"}
    {:mean 3.201975370229313E-4, :feature_variable "CD163", :final_diagnosis "PXA"}
    {:mean 4.608615134169755E-4, :feature_variable "TMEM119", :final_diagnosis "PXA"}
    {:mean 1.0591251900975978E-4, :feature_variable "CD209", :final_diagnosis "PXA"}
    {:mean 1.2554157928055298E-4, :feature_variable "CD141", :final_diagnosis "PXA"}
    {:mean 1.3949244784582733E-4, :feature_variable "CD3", :final_diagnosis "PXA"}
    {:mean 4.534552884859291E-4, :feature_variable "CD68", :final_diagnosis "PXA"}
    {:mean 1.0427547149032535E-4, :feature_variable "IDO1", :final_diagnosis "PXA"}
    {:mean 1.800928715011245E-5, :feature_variable "CD31", :final_diagnosis "PXA"}
    {:mean 8.153125885276546E-5, :feature_variable "IDH1_R132H", :final_diagnosis "Astrocytoma"}
    {:mean 6.775259966000786E-4, :feature_variable "PDL1", :final_diagnosis "Astrocytoma"}
    {:mean 4.45748788780759E-6, :feature_variable "Chym_Tryp", :final_diagnosis "Astrocytoma"}
    {:mean 2.8380068012192076E-4, :feature_variable "GFAP", :final_diagnosis "Astrocytoma"}
    {:mean 5.158828474179773E-4, :feature_variable "NeuN", :final_diagnosis "Astrocytoma"}
    {:mean 2.5683230878284404E-4, :feature_variable "H3K27me3", :final_diagnosis "Astrocytoma"}
    {:mean 6.80871377267946E-4, :feature_variable "CD45", :final_diagnosis "Astrocytoma"}
    {:mean 5.10543324608247E-4, :feature_variable "CD68", :final_diagnosis "Astrocytoma"}
    {:mean 7.001269559385264E-7, :feature_variable "LAG3", :final_diagnosis "Astrocytoma"}
    {:mean 0.0011051625444186224, :feature_variable "HLA1", :final_diagnosis "Astrocytoma"}
    {:mean 1.1763872001840998E-4, :feature_variable "CD141", :final_diagnosis "Astrocytoma"}
    {:mean 4.913489471993674E-4, :feature_variable "TIM3", :final_diagnosis "Astrocytoma"}
    {:mean 1.2373847024306463E-5, :feature_variable "CD31", :final_diagnosis "Astrocytoma"}
    {:mean 3.595990575152695E-4, :feature_variable "HLADR", :final_diagnosis "Astrocytoma"}
    {:mean 5.762795234330644E-4, :feature_variable "CD14", :final_diagnosis "Astrocytoma"}
    {:mean 5.611037251167273E-4, :feature_variable "Ki67", :final_diagnosis "Astrocytoma"}
    {:mean 6.733099499549625E-5, :feature_variable "Olig2", :final_diagnosis "Astrocytoma"}
    {:mean 6.542334598475082E-6, :feature_variable "VISTA", :final_diagnosis "Astrocytoma"}
    {:mean 3.144030586433946E-4, :feature_variable "CD47", :final_diagnosis "Astrocytoma"}
    {:mean 3.978914938991701E-4, :feature_variable "Tox", :final_diagnosis "Astrocytoma"}
    {:mean 7.591676187730732E-5, :feature_variable "ICOS", :final_diagnosis "Astrocytoma"}
    {:mean 1.527459195368686E-4, :feature_variable "GM2_GD2", :final_diagnosis "Astrocytoma"}
    {:mean 9.334389602536574E-5, :feature_variable "CD40", :final_diagnosis "PXA"}
    {:mean 1.8285082189127562E-5, :feature_variable "EGFRvIII", :final_diagnosis "PXA"}
    {:mean 3.436061428853926E-5, :feature_variable "EphA2", :final_diagnosis "PXA"}
    {:mean 9.720880236302229E-5, :feature_variable "FoxP3", :final_diagnosis "PXA"}
    {:mean 3.530444332980337E-5, :feature_variable "ICOS", :final_diagnosis "PXA"}
    {:mean 3.851092867432695E-5, :feature_variable "GPC2", :final_diagnosis "PXA"}
    {:mean 2.0558140605985282E-4, :feature_variable "NeuN", :final_diagnosis "PXA"}
    {:mean 5.355204885612332E-5, :feature_variable "CD208", :final_diagnosis "PXA"}
    {:mean 0.0, :feature_variable "H3K27M", :final_diagnosis "PXA"}
    {:mean 2.9714613517820074E-5, :feature_variable "NG2", :final_diagnosis "PXA"}
    {:mean 0.0015620663141277184, :feature_variable "HLA1", :final_diagnosis "PXA"}
    {:mean 3.747021611055371E-4, :feature_variable "CD206", :final_diagnosis "Oligodendroglioma"}
    {:mean 4.3730645831398793E-4, :feature_variable "CD45", :final_diagnosis "Oligodendroglioma"}
    {:mean 8.864396427602122E-6, :feature_variable "CD31", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.3233909860088123E-4, :feature_variable "HLADR", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.9049458012415713E-4, :feature_variable "CD8", :final_diagnosis "Oligodendroglioma"}
    {:mean 5.61322948439566E-4, :feature_variable "Ki67", :final_diagnosis "Oligodendroglioma"}
    {:mean 4.4260480191989E-4, :feature_variable "CD38", :final_diagnosis "Oligodendroglioma"}
    {:mean 7.420451911993387E-5, :feature_variable "CD4", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.3916034644159754E-4, :feature_variable "CD86", :final_diagnosis "Oligodendroglioma"}
    {:mean 5.91762779351134E-5, :feature_variable "GPC2", :final_diagnosis "Oligodendroglioma"}
    {:mean 0.001965574829869043, :feature_variable "PDL1", :final_diagnosis "Oligodendroglioma"}
    {:mean 4.7832937017369436E-5, :feature_variable "HER2", :final_diagnosis "Oligodendroglioma"}
    {:mean 6.520138276296356E-5, :feature_variable "Olig2", :final_diagnosis "Oligodendroglioma"}
    {:mean 7.628966548376634E-4, :feature_variable "TMEM119", :final_diagnosis "Oligodendroglioma"}
    {:mean 6.2538903519933365E-6, :feature_variable "VISTA", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.1544520384874605E-4, :feature_variable "CD40", :final_diagnosis "Oligodendroglioma"}
    {:mean 8.161080462439409E-5,
     :feature_variable "IDH1_R132H",
     :final_diagnosis "Oligodendroglioma"}
    {:mean 3.8904180122580957E-4, :feature_variable "CD14", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.4120517116514107E-4, :feature_variable "CD209", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.5333759233047445E-4, :feature_variable "GM2_GD2", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.5739772391747944E-4, :feature_variable "B7H3", :final_diagnosis "Oligodendroglioma"}
    {:mean 9.578456251183213E-5, :feature_variable "ICOS", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.959620381958994E-4, :feature_variable "H3K27me3", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.2444561649256605E-4, :feature_variable "IDO1", :final_diagnosis "Oligodendroglioma"}
    {:mean 4.2398354996223874E-5,
     :feature_variable "EGFRvIII",
     :final_diagnosis "Oligodendroglioma"}
    {:mean 1.316185090861821E-4, :feature_variable "CD208", :final_diagnosis "Oligodendroglioma"}
    {:mean 4.8735274043736055E-4, :feature_variable "CD11b", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.0760807027805488E-4, :feature_variable "GFAP", :final_diagnosis "Oligodendroglioma"}
    {:mean 3.87627107845126E-5, :feature_variable "NG2", :final_diagnosis "Oligodendroglioma"}
    {:mean 7.105529904916357E-5, :feature_variable "CD3", :final_diagnosis "Oligodendroglioma"}
    {:mean 3.3177137481072673E-6,
     :feature_variable "Chym_Tryp",
     :final_diagnosis "Oligodendroglioma"}
    {:mean 1.1397712762826582E-4, :feature_variable "EGFR", :final_diagnosis "Oligodendroglioma"}
    {:mean 5.724351912874308E-4, :feature_variable "HLA1", :final_diagnosis "Oligodendroglioma"}
    {:mean 4.824382836967574E-4, :feature_variable "TIM3", :final_diagnosis "Oligodendroglioma"}
    {:mean 5.543138645780994E-4, :feature_variable "Tox", :final_diagnosis "Oligodendroglioma"}
    {:mean 0.0010452239558838407, :feature_variable "CD123", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.783763252693995E-5, :feature_variable "CD133", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.388220996624963E-4, :feature_variable "GLUT1", :final_diagnosis "Oligodendroglioma"}
    {:mean 6.976231424371732E-5, :feature_variable "EphA2", :final_diagnosis "Oligodendroglioma"}
    {:mean 6.558152354701706E-4, :feature_variable "NeuN", :final_diagnosis "Oligodendroglioma"}
    {:mean 7.829556474802527E-4, :feature_variable "CD47", :final_diagnosis "Oligodendroglioma"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "Oligodendroglioma"}
    {:mean 6.13172233302016E-4, :feature_variable "CD68", :final_diagnosis "Oligodendroglioma"}
    {:mean 3.5965351476896707E-4, :feature_variable "CD163", :final_diagnosis "Oligodendroglioma"}
    {:mean 0.0, :feature_variable "H3K27M", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.2062388799227485E-4, :feature_variable "CD141", :final_diagnosis "Oligodendroglioma"}
    {:mean 9.089281069382356E-5, :feature_variable "FoxP3", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.4478343078436106E-5, :feature_variable "CD133", :final_diagnosis "Normal_brain"}
    {:mean 1.269554877844559E-4, :feature_variable "CD8", :final_diagnosis "Normal_brain"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "Normal_brain"}
    {:mean 9.037238296765755E-5, :feature_variable "PDL1", :final_diagnosis "Normal_brain"}
    {:mean 1.0139292619952473E-4, :feature_variable "B7H3", :final_diagnosis "Normal_brain"}
    {:mean 3.976151906124573E-4, :feature_variable "CD206", :final_diagnosis "Normal_brain"}
    {:mean 9.31515049141152E-5, :feature_variable "CD40", :final_diagnosis "Normal_brain"}
    {:mean 4.520832475319797E-5, :feature_variable "CD4", :final_diagnosis "Normal_brain"}
    {:mean 1.1089601616693444E-4, :feature_variable "Calprotectin", :final_diagnosis "Normal_brain"}
    {:mean 3.0654674925677495E-5, :feature_variable "EGFRvIII", :final_diagnosis "Normal_brain"}
    {:mean 2.931291977625498E-5, :feature_variable "EphA2", :final_diagnosis "Normal_brain"}
    {:mean 2.0638969460611616E-4, :feature_variable "GFAP", :final_diagnosis "Normal_brain"}
    {:mean 3.574718766672957E-4, :feature_variable "CD14", :final_diagnosis "Normal_brain"}
    {:mean 9.399181468047961E-5, :feature_variable "CD141", :final_diagnosis "Normal_brain"}
    {:mean 5.320280197832977E-5, :feature_variable "CD3", :final_diagnosis "Normal_brain"}
    {:mean 5.649453071483325E-5, :feature_variable "IDO1", :final_diagnosis "Normal_brain"}
    {:mean 4.573962905548712E-4, :feature_variable "CD163", :final_diagnosis "Normal_brain"}
    {:mean 2.2481985670767183E-4, :feature_variable "GLUT1", :final_diagnosis "Normal_brain"}
    {:mean 7.533863241030329E-6, :feature_variable "CD31", :final_diagnosis "Normal_brain"}
    {:mean 6.312161766289609E-6, :feature_variable "VISTA", :final_diagnosis "Normal_brain"}
    {:mean 1.9444649966870198E-4, :feature_variable "Ki67", :final_diagnosis "Normal_brain"}
    {:mean 3.844637192844515E-5, :feature_variable "EGFR", :final_diagnosis "Normal_brain"}
    {:mean 2.5530216421329493E-4, :feature_variable "ApoE", :final_diagnosis "Oligodendroglioma"}
    {:mean 1.4643132022996392E-4,
     :feature_variable "Calprotectin",
     :final_diagnosis "Oligodendroglioma"}
    {:mean 1.1514492969957161E-4, :feature_variable "CD208", :final_diagnosis "Normal_brain"}
    {:mean 4.3972328992819846E-4, :feature_variable "CD68", :final_diagnosis "Normal_brain"}
    {:mean 0.0, :feature_variable "PD1", :final_diagnosis "Normal_brain"}
    {:mean 8.126037566089516E-5, :feature_variable "IDH1_R132H", :final_diagnosis "Normal_brain"}
    {:mean 3.2544153748536507E-4, :feature_variable "NeuN", :final_diagnosis "Normal_brain"}
    {:mean 1.6690753595974153E-4, :feature_variable "GM2_GD2", :final_diagnosis "Normal_brain"}
    {:mean 1.6291095326420783E-4, :feature_variable "H3K27me3", :final_diagnosis "Normal_brain"}
    {:mean 1.4851987175298727E-4, :feature_variable "ApoE", :final_diagnosis "Normal_brain"}
    {:mean 4.3295621782953655E-5, :feature_variable "GPC2", :final_diagnosis "Normal_brain"}
    {:mean 8.634555033861139E-4, :feature_variable "CD123", :final_diagnosis "Normal_brain"}
    {:mean 7.803309127242897E-4, :feature_variable "TMEM119", :final_diagnosis "Normal_brain"}
    {:mean 1.6840901922660613E-4, :feature_variable "CD209", :final_diagnosis "Normal_brain"}
    {:mean 4.330028925003996E-5, :feature_variable "NG2", :final_diagnosis "Normal_brain"}
    {:mean 3.2831952326506744E-4, :feature_variable "Tox", :final_diagnosis "Normal_brain"}
    {:mean 4.480467235212597E-4, :feature_variable "CD45", :final_diagnosis "Normal_brain"}
    {:mean 3.962302646099115E-6, :feature_variable "Chym_Tryp", :final_diagnosis "Normal_brain"}
    {:mean 4.034707618724635E-5, :feature_variable "Olig2", :final_diagnosis "pGBM"}
    {:mean 2.637289537840913E-4, :feature_variable "CD123", :final_diagnosis "pGBM"}
    {:mean 2.292876003226409E-4, :feature_variable "TMEM119", :final_diagnosis "pGBM"}
    {:mean 6.523222429516846E-5, :feature_variable "CD209", :final_diagnosis "pGBM"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "pGBM"}
    {:mean 1.6500419803501658E-5, :feature_variable "CD31", :final_diagnosis "pGBM"}
    {:mean 1.1214095272469904E-5, :feature_variable "PD1", :final_diagnosis "pGBM"}
    {:mean 1.9895170290346202E-4, :feature_variable "CD11b", :final_diagnosis "pGBM"}
    {:mean 7.040789816697218E-5, :feature_variable "H3K27M", :final_diagnosis "pGBM"}
    {:mean 2.9225719005805715E-5, :feature_variable "FoxP3", :final_diagnosis "pGBM"}
    {:mean 3.429311638527972E-5, :feature_variable "CD4", :final_diagnosis "pGBM"}
    {:mean 4.675163527173923E-5, :feature_variable "CD206", :final_diagnosis "pGBM"}
    {:mean 2.6145065615431923E-4, :feature_variable "CD163", :final_diagnosis "pGBM"}
    {:mean 5.450675930194636E-5, :feature_variable "CD86", :final_diagnosis "pGBM"}
    {:mean 2.85070825620236E-4, :feature_variable "HLADR", :final_diagnosis "pGBM"}
    {:mean 9.240306055651757E-5, :feature_variable "EGFR", :final_diagnosis "pGBM"}
    {:mean 5.497631635810528E-4, :feature_variable "CD14", :final_diagnosis "pGBM"}
    {:mean 1.960366804431725E-4, :feature_variable "ApoE", :final_diagnosis "pGBM"}
    {:mean 4.7763476541310844E-5, :feature_variable "Calprotectin", :final_diagnosis "pGBM"}
    {:mean 2.3126922738776908E-4, :feature_variable "NeuN", :final_diagnosis "pGBM"}
    {:mean 1.1681169511614694E-4, :feature_variable "PDL1", :final_diagnosis "pGBM"}
    {:mean 9.315422109306048E-5, :feature_variable "CD38", :final_diagnosis "pGBM"}
    {:mean 4.967693652459355E-4, :feature_variable "CD45", :final_diagnosis "pGBM"}
    {:mean 4.600292656030855E-5, :feature_variable "GPC2", :final_diagnosis "pGBM"}
    {:mean 1.1230960963803946E-4, :feature_variable "IDH1_R132H", :final_diagnosis "pGBM"}
    {:mean 3.145503376510808E-4, :feature_variable "CD68", :final_diagnosis "pGBM"}
    {:mean 1.2385208526218775E-4, :feature_variable "GLUT1", :final_diagnosis "pGBM"}
    {:mean 4.072321898398609E-5, :feature_variable "NG2", :final_diagnosis "pGBM"}
    {:mean 1.1836650762754312E-5, :feature_variable "ICOS", :final_diagnosis "pGBM"}
    {:mean 1.8319267633072552E-4, :feature_variable "CD47", :final_diagnosis "pGBM"}
    {:mean 2.692263920526627E-5, :feature_variable "EphA2", :final_diagnosis "pGBM"}
    {:mean 7.3916833876358E-5, :feature_variable "CD208", :final_diagnosis "pGBM"}
    {:mean 1.2255360364564995E-4, :feature_variable "Tox", :final_diagnosis "pGBM"}
    {:mean 5.019536938027908E-5, :feature_variable "HER2", :final_diagnosis "pGBM"}
    {:mean 2.4513068854737847E-5, :feature_variable "CD133", :final_diagnosis "pGBM"}
    {:mean 7.861866317098499E-4, :feature_variable "Ki67", :final_diagnosis "pGBM"}
    {:mean 1.5084749544097087E-4, :feature_variable "TIM3", :final_diagnosis "pGBM"}
    {:mean 2.469385023146511E-4, :feature_variable "B7H3", :final_diagnosis "pGBM"}
    {:mean 7.352482963155484E-6, :feature_variable "VISTA", :final_diagnosis "pGBM"}
    {:mean 6.0902054108160254E-5, :feature_variable "CD3", :final_diagnosis "pGBM"}
    {:mean 1.5880465311921715E-4, :feature_variable "H3K27me3", :final_diagnosis "pGBM"}
    {:mean 0.00112823228937126, :feature_variable "HLA1", :final_diagnosis "pGBM"}
    {:mean 4.226639353462315E-4, :feature_variable "GFAP", :final_diagnosis "pGBM"}
    {:mean 9.381901897679249E-5, :feature_variable "CD8", :final_diagnosis "pGBM"}
    {:mean 7.771900588482868E-5, :feature_variable "CD141", :final_diagnosis "pGBM"}
    {:mean 1.4451481056290191E-5, :feature_variable "EGFRvIII", :final_diagnosis "pGBM"}
    {:mean 2.966252557521065E-6, :feature_variable "Chym_Tryp", :final_diagnosis "pGBM"}
    {:mean 2.523570204008766E-5, :feature_variable "IDO1", :final_diagnosis "pGBM"}
    {:mean 7.4375719709953E-5, :feature_variable "CD3", :final_diagnosis "Thalmic_glioma"}
    {:mean 3.1014136913284934E-5,
     :feature_variable "Calprotectin",
     :final_diagnosis "Thalmic_glioma"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "Thalmic_glioma"}
    {:mean 3.644236630176951E-5, :feature_variable "CD208", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.633462034398222E-5, :feature_variable "NG2", :final_diagnosis "Thalmic_glioma"}
    {:mean 9.656933428469634E-6, :feature_variable "CD31", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.5257393717764444E-5, :feature_variable "GPC2", :final_diagnosis "Thalmic_glioma"}
    {:mean 4.783981312463477E-4, :feature_variable "TMEM119", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.9306700847972154E-4, :feature_variable "NeuN", :final_diagnosis "Thalmic_glioma"}
    {:mean 4.1223223718075E-4, :feature_variable "CD38", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.8311873733558722E-4, :feature_variable "CD163", :final_diagnosis "Thalmic_glioma"}
    {:mean 7.905337321489958E-5, :feature_variable "CD47", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.2262967437693427E-4, :feature_variable "CD11b", :final_diagnosis "Thalmic_glioma"}
    {:mean 4.0762629818328885E-5, :feature_variable "TIM3", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.3547036370758516E-4, :feature_variable "B7H3", :final_diagnosis "Thalmic_glioma"}
    {:mean 6.119803199942E-5, :feature_variable "HER2", :final_diagnosis "Thalmic_glioma"}
    {:mean 4.086626811747884E-5, :feature_variable "CD209", :final_diagnosis "Thalmic_glioma"}
    {:mean 7.1907559744615936E-6, :feature_variable "VISTA", :final_diagnosis "Thalmic_glioma"}
    {:mean 6.851869482524678E-4, :feature_variable "CD45", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.2383568696371026E-4, :feature_variable "HLADR", :final_diagnosis "Thalmic_glioma"}
    {:mean 0.0, :feature_variable "EGFR", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.554937792476373E-4, :feature_variable "GFAP", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.286623251602123E-4, :feature_variable "CD123", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.736610081190307E-5, :feature_variable "CD4", :final_diagnosis "Thalmic_glioma"}
    {:mean 0.0, :feature_variable "PD1", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.959607942611811E-5, :feature_variable "CD133", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.9772016863306198E-5, :feature_variable "CD206", :final_diagnosis "Thalmic_glioma"}
    {:mean 3.0074228106865977E-5, :feature_variable "GM2_GD2", :final_diagnosis "pGBM"}
    {:mean 2.2222062169186704E-4, :feature_variable "ApoE", :final_diagnosis "Glioma"}
    {:mean 2.1243001941260845E-4, :feature_variable "NeuN", :final_diagnosis "Glioma"}
    {:mean 7.635162933735354E-4, :feature_variable "HLA1", :final_diagnosis "Glioma"}
    {:mean 0.0, :feature_variable "ICOS", :final_diagnosis "Glioma"}
    {:mean 5.2463960590597966E-5, :feature_variable "CD4", :final_diagnosis "Glioma"}
    {:mean 1.4307337761960544E-4, :feature_variable "CD8", :final_diagnosis "Glioma"}
    {:mean 1.0036727787697816E-4, :feature_variable "Tox", :final_diagnosis "Glioma"}
    {:mean 6.143846182264175E-5, :feature_variable "CD3", :final_diagnosis "Glioma"}
    {:mean 4.133968592896341E-4, :feature_variable "GFAP", :final_diagnosis "Glioma"}
    {:mean 6.2115677455174085E-6, :feature_variable "PD1", :final_diagnosis "Glioma"}
    {:mean 3.724460793248915E-5, :feature_variable "Calprotectin", :final_diagnosis "Glioma"}
    {:mean 2.3563184464147472E-4, :feature_variable "HLADR", :final_diagnosis "Glioma"}
    {:mean 3.3631826641291766E-4, :feature_variable "CD163", :final_diagnosis "Glioma"}
    {:mean 5.899731621233849E-4, :feature_variable "CD14", :final_diagnosis "Glioma"}
    {:mean 5.599457186535094E-5, :feature_variable "IDO1", :final_diagnosis "Glioma"}
    {:mean 4.298059029578078E-4, :feature_variable "CD45", :final_diagnosis "Glioma"}
    {:mean 8.401844031023832E-5, :feature_variable "CD4", :final_diagnosis "PXA"}
    {:mean 8.107464630531905E-6, :feature_variable "PD1", :final_diagnosis "PXA"}
    {:mean 1.7945480218753356E-4, :feature_variable "H3K27me3", :final_diagnosis "PXA"}
    {:mean 5.294542506654796E-5, :feature_variable "CD40", :final_diagnosis "pGBM"}
    {:mean 4.2909123420644485E-5, :feature_variable "CD40", :final_diagnosis "Thalmic_glioma"}
    {:mean 5.570278054561243E-5, :feature_variable "Olig2", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.883098593189484E-4, :feature_variable "CD68", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.5631880250379382E-4, :feature_variable "PDL1", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.8715176865726868E-4, :feature_variable "GLUT1", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.0048382505374381E-4, :feature_variable "Tox", :final_diagnosis "Thalmic_glioma"}
    {:mean 4.6660369187101247E-4, :feature_variable "CD14", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.565849361841778E-5, :feature_variable "FoxP3", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.8045607764099862E-4, :feature_variable "CD8", :final_diagnosis "Thalmic_glioma"}
    {:mean 0.0, :feature_variable "EGFRvIII", :final_diagnosis "Thalmic_glioma"}
    {:mean 9.661638787112378E-5, :feature_variable "CD86", :final_diagnosis "Thalmic_glioma"}
    {:mean 0.0, :feature_variable "EphA2", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.885707508599609E-4, :feature_variable "H3K27me3", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.4058071014287448E-4, :feature_variable "ApoE", :final_diagnosis "Thalmic_glioma"}
    {:mean 0.0, :feature_variable "H3K27M", :final_diagnosis "Thalmic_glioma"}
    {:mean 5.518238857141138E-5, :feature_variable "CD141", :final_diagnosis "Thalmic_glioma"}
    {:mean 5.310997282271578E-6, :feature_variable "PD1", :final_diagnosis "pHGG"}
    {:mean 7.616478771411927E-5, :feature_variable "CD3", :final_diagnosis "pHGG"}
    {:mean 2.1384348944357706E-5, :feature_variable "CD133", :final_diagnosis "pHGG"}
    {:mean 3.453676446933334E-5, :feature_variable "Olig2", :final_diagnosis "pHGG"}
    {:mean 7.845743012582692E-5, :feature_variable "IDH1_R132H", :final_diagnosis "pHGG"}
    {:mean 8.064478877320281E-4, :feature_variable "Ki67", :final_diagnosis "pHGG"}
    {:mean 4.4102986266161804E-5, :feature_variable "CD208", :final_diagnosis "pHGG"}
    {:mean 7.39528806808942E-6, :feature_variable "Chym_Tryp", :final_diagnosis "pHGG"}
    {:mean 1.1529929939814211E-5, :feature_variable "GM2_GD2", :final_diagnosis "pHGG"}
    {:mean 3.696057542588465E-5, :feature_variable "NG2", :final_diagnosis "pHGG"}
    {:mean 1.7446389041490725E-4, :feature_variable "B7H3", :final_diagnosis "pHGG"}
    {:mean 6.719911058762159E-6, :feature_variable "VISTA", :final_diagnosis "pHGG"}
    {:mean 1.7210598794723333E-4, :feature_variable "GLUT1", :final_diagnosis "pHGG"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "pHGG"}
    {:mean 3.799251952169751E-4, :feature_variable "HLADR", :final_diagnosis "pHGG"}
    {:mean 4.771972302669309E-5, :feature_variable "GPC2", :final_diagnosis "pHGG"}
    {:mean 1.787029445392483E-4, :feature_variable "TIM3", :final_diagnosis "pHGG"}
    {:mean 1.121137700275097E-4, :feature_variable "CD123", :final_diagnosis "pHGG"}
    {:mean 8.077716606294717E-5, :feature_variable "CD209", :final_diagnosis "pHGG"}
    {:mean 5.572060286793275E-5, :feature_variable "CD206", :final_diagnosis "pHGG"}
    {:mean 6.561038330630841E-5, :feature_variable "CD40", :final_diagnosis "pHGG"}
    {:mean 0.0, :feature_variable "H3K27M", :final_diagnosis "pHGG"}
    {:mean 1.4302357965877409E-5, :feature_variable "CD31", :final_diagnosis "pHGG"}
    {:mean 4.35191498943112E-4, :feature_variable "TMEM119", :final_diagnosis "pHGG"}
    {:mean 1.9799429086624586E-4, :feature_variable "H3K27me3", :final_diagnosis "pHGG"}
    {:mean 3.886336158951444E-5, :feature_variable "HER2", :final_diagnosis "pHGG"}
    {:mean 6.901550801554474E-5, :feature_variable "CD38", :final_diagnosis "pHGG"}
    {:mean 4.081011308493963E-5, :feature_variable "IDO1", :final_diagnosis "pHGG"}
    {:mean 1.407231078641E-5, :feature_variable "EGFRvIII", :final_diagnosis "pHGG"}
    {:mean 4.5601849301279396E-5, :feature_variable "FoxP3", :final_diagnosis "pHGG"}
    {:mean 4.783645529136938E-5, :feature_variable "EGFR", :final_diagnosis "pHGG"}
    {:mean 9.857685657356623E-5, :feature_variable "CD8", :final_diagnosis "pHGG"}
    {:mean 6.0913445192808946E-5, :feature_variable "CD86", :final_diagnosis "pHGG"}
    {:mean 6.104892170752592E-4, :feature_variable "CD14", :final_diagnosis "pHGG"}
    {:mean 5.311562451463158E-6, :feature_variable "ICOS", :final_diagnosis "pHGG"}
    {:mean 1.308625162776593E-4, :feature_variable "Calprotectin", :final_diagnosis "pHGG"}
    {:mean 1.4988144532824505E-4, :feature_variable "ApoE", :final_diagnosis "pHGG"}
    {:mean 2.0362932719931844E-5, :feature_variable "EphA2", :final_diagnosis "pHGG"}
    {:mean 2.333380444148751E-4, :feature_variable "NeuN", :final_diagnosis "pHGG"}
    {:mean 4.886677462277841E-5, :feature_variable "CD4", :final_diagnosis "pHGG"}
    {:mean 5.123274051057326E-5, :feature_variable "CD4", :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 6.507768304590207E-5,
     :feature_variable "GM2_GD2",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 2.4669328358101574E-4,
     :feature_variable "ApoE",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 8.384194393907706E-5,
     :feature_variable "HER2",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 4.315641231332086E-5,
     :feature_variable "Calprotectin",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.648699187033981E-5,
     :feature_variable "CD31",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 3.137971794352319E-5,
     :feature_variable "EphA2",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 5.76357747837074E-4, :feature_variable "CD14", :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 6.725399666357479E-5,
     :feature_variable "CD141",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.58021647886171E-4, :feature_variable "Tox", :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 7.557434705847634E-4,
     :feature_variable "H3K27M",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 2.8900102990971657E-4,
     :feature_variable "CD163",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 5.4991921646926206E-5,
     :feature_variable "CD86",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 7.067366890512196E-5,
     :feature_variable "CD209",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.8682661118743563E-6,
     :feature_variable "Chym_Tryp",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.1217996926321943E-4,
     :feature_variable "IDH1_R132H",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 3.7374579434373866E-4,
     :feature_variable "TMEM119",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 7.160755016838219E-4,
     :feature_variable "GFAP",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 2.3596694263513756E-4, :feature_variable "PDL1", :final_diagnosis "pHGG"}
    {:mean 0.0010422500256793389, :feature_variable "HLA1", :final_diagnosis "pHGG"}
    {:mean 1.0131448098696201E-4, :feature_variable "CD141", :final_diagnosis "pHGG"}
    {:mean 2.0710918743220897E-4, :feature_variable "GFAP", :final_diagnosis "pHGG"}
    {:mean 5.824582436361731E-4, :feature_variable "CD45", :final_diagnosis "pHGG"}
    {:mean 1.873339220969925E-4, :feature_variable "CD163", :final_diagnosis "pHGG"}
    {:mean 1.028075785172709E-4, :feature_variable "CD47", :final_diagnosis "pHGG"}
    {:mean 3.9141827831498015E-4, :feature_variable "CD68", :final_diagnosis "pHGG"}
    {:mean 2.1360796879115045E-4, :feature_variable "CD11b", :final_diagnosis "pHGG"}
    {:mean 1.2433464396648824E-4,
     :feature_variable "CD8",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 6.584819538741797E-4,
     :feature_variable "CD45",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.733409132161194E-4,
     :feature_variable "TIM3",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 0.0, :feature_variable "PD1", :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 4.666190620184927E-5, :feature_variable "NG2", :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 3.7448868513931954E-4,
     :feature_variable "CD68",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 8.379386605653214E-6,
     :feature_variable "VISTA",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.4337179305856478E-4,
     :feature_variable "CD47",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 2.1574110676832276E-4,
     :feature_variable "H3K27me3",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 7.234335638830095E-5,
     :feature_variable "CD206",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 5.962073372188704E-4,
     :feature_variable "Ki67",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 0.0010391684824227925,
     :feature_variable "HLA1",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 3.2190973454025343E-5,
     :feature_variable "CD133",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.9267193101012286E-4,
     :feature_variable "B7H3",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.7026532561351617E-4, :feature_variable "Tox", :final_diagnosis "pHGG"}
    {:mean 5.771073018458268E-5, :feature_variable "CD3", :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 4.3041977613887374E-4,
     :feature_variable "CD123",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 5.060843991405258E-5,
     :feature_variable "FoxP3",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 3.8468353248427296E-4,
     :feature_variable "HLADR",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 6.658219489074649E-6,
     :feature_variable "EGFRvIII",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 2.120222615702219E-4,
     :feature_variable "CD11b",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 3.366513344043954E-4,
     :feature_variable "NeuN",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 9.966816700129813E-5,
     :feature_variable "CD208",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 2.6253235481202487E-4,
     :feature_variable "GLUT1",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 4.58475806588576E-5, :feature_variable "GPC2", :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 4.246201474967914E-5,
     :feature_variable "ICOS",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 6.030301761640477E-5,
     :feature_variable "Olig2",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 2.1164745283928864E-4,
     :feature_variable "CD38",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 4.038842577422084E-5,
     :feature_variable "LAG3",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 1.7359748184894017E-4,
     :feature_variable "PDL1",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 6.913098029117374E-5,
     :feature_variable "CD40",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 6.747648428969402E-5, :feature_variable "CD141", :final_diagnosis "Glioma"}
    {:mean 1.7260506731116778E-4, :feature_variable "CD11b", :final_diagnosis "Glioma"}
    {:mean 1.8147281013413862E-6, :feature_variable "Chym_Tryp", :final_diagnosis "Glioma"}
    {:mean 9.622537048639534E-5, :feature_variable "PDL1", :final_diagnosis "Glioma"}
    {:mean 1.0046964146979277E-4, :feature_variable "CD208", :final_diagnosis "Glioma"}
    {:mean 1.0405160976504574E-4, :feature_variable "CD206", :final_diagnosis "Glioma"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "Glioma"}
    {:mean 1.5516153922491162E-4, :feature_variable "H3K27me3", :final_diagnosis "Glioma"}
    {:mean 5.252213969550158E-5, :feature_variable "CD38", :final_diagnosis "Glioma"}
    {:mean 2.9058200152399942E-5, :feature_variable "Olig2", :final_diagnosis "Glioma"}
    {:mean 1.5280478345173895E-5, :feature_variable "CD133", :final_diagnosis "Glioma"}
    {:mean 3.666975425143503E-4, :feature_variable "CD68", :final_diagnosis "Glioma"}
    {:mean 7.361421109142088E-5, :feature_variable "CD86", :final_diagnosis "Glioma"}
    {:mean 3.2230914519361336E-5, :feature_variable "GM2_GD2", :final_diagnosis "Glioma"}
    {:mean 9.949596638542353E-5, :feature_variable "IDH1_R132H", :final_diagnosis "Glioma"}
    {:mean 1.5819742398772934E-4, :feature_variable "CD47", :final_diagnosis "Glioma"}
    {:mean 2.2036828225339302E-4, :feature_variable "TMEM119", :final_diagnosis "Glioma"}
    {:mean 0.0, :feature_variable "H3K27M", :final_diagnosis "Glioma"}
    {:mean 5.093889779978542E-6, :feature_variable "EGFR", :final_diagnosis "Glioma"}
    {:mean 6.558320447916892E-5, :feature_variable "GLUT1", :final_diagnosis "Glioma"}
    {:mean 7.2579307923447664E-6, :feature_variable "VISTA", :final_diagnosis "Glioma"}
    {:mean 1.3254072619359135E-4, :feature_variable "TIM3", :final_diagnosis "Glioma"}
    {:mean 3.5038747052483E-5, :feature_variable "EphA2", :final_diagnosis "Glioma"}
    {:mean 4.6494320297004745E-5, :feature_variable "CD209", :final_diagnosis "Glioma"}
    {:mean 7.167415143896806E-5, :feature_variable "HER2", :final_diagnosis "Glioma"}
    {:mean 6.654367868876947E-4, :feature_variable "Ki67", :final_diagnosis "Glioma"}
    {:mean 4.280146427816578E-5, :feature_variable "NG2", :final_diagnosis "Glioma"}
    {:mean 6.180536573415984E-5,
     :feature_variable "EGFR",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 4.938009238152618E-5, :feature_variable "GPC2", :final_diagnosis "Glioma"}
    {:mean 3.3355959504252197E-4, :feature_variable "CD123", :final_diagnosis "Glioma"}
    {:mean 2.219135809540994E-5, :feature_variable "CD31", :final_diagnosis "Glioma"}
    {:mean 7.274884611984677E-5, :feature_variable "EGFRvIII", :final_diagnosis "Glioma"}
    {:mean 8.641295798839316E-6, :feature_variable "PD1", :final_diagnosis "Oligodendroglioma"}
    {:mean 2.2602070198041047E-4, :feature_variable "CD47", :final_diagnosis "Normal_brain"}
    {:mean 5.395984398751051E-4, :feature_variable "HLA1", :final_diagnosis "Normal_brain"}
    {:mean 1.1552667899297699E-4, :feature_variable "CD38", :final_diagnosis "Normal_brain"}
    {:mean 4.419308309648607E-5, :feature_variable "FoxP3", :final_diagnosis "Normal_brain"}
    {:mean 1.791357244983046E-4, :feature_variable "HLADR", :final_diagnosis "Normal_brain"}
    {:mean 5.954697768408014E-5, :feature_variable "CD86", :final_diagnosis "Normal_brain"}
    {:mean 3.4359540418704373E-4, :feature_variable "CD11b", :final_diagnosis "Normal_brain"}
    {:mean 2.3792999261196737E-4, :feature_variable "TIM3", :final_diagnosis "Normal_brain"}
    {:mean 5.3200505207967826E-5, :feature_variable "Olig2", :final_diagnosis "Normal_brain"}
    {:mean 4.404492757005276E-5, :feature_variable "HER2", :final_diagnosis "Normal_brain"}
    {:mean 0.0, :feature_variable "H3K27M", :final_diagnosis "Normal_brain"}
    {:mean 1.4071391063222549E-5, :feature_variable "ICOS", :final_diagnosis "Normal_brain"}
    {:mean 6.086944164873524E-6, :feature_variable "CD31", :final_diagnosis "Ganglioglioma"}
    {:mean 2.639941347045374E-5, :feature_variable "CD4", :final_diagnosis "Ganglioglioma"}
    {:mean 6.358445775368223E-4, :feature_variable "HLA1", :final_diagnosis "Ganglioglioma"}
    {:mean 6.583715974237619E-5, :feature_variable "NG2", :final_diagnosis "Ganglioglioma"}
    {:mean 3.194120493671059E-4, :feature_variable "CD8", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "NeuN", :final_diagnosis "Ganglioglioma"}
    {:mean 5.099472825840001E-5, :feature_variable "Tox", :final_diagnosis "Ganglioglioma"}
    {:mean 7.6231296190323235E-6, :feature_variable "VISTA", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "EGFR", :final_diagnosis "Ganglioglioma"}
    {:mean 5.687523676758679E-4, :feature_variable "CD14", :final_diagnosis "Ganglioglioma"}
    {:mean 6.351547009239228E-5, :feature_variable "CD3", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "CD38", :final_diagnosis "Ganglioglioma"}
    {:mean 2.6754308589181862E-5,
     :feature_variable "Calprotectin",
     :final_diagnosis "Ganglioglioma"}
    {:mean 9.884485442791761E-5, :feature_variable "PD1", :final_diagnosis "Ganglioglioma"}
    {:mean 2.3993064878249822E-5, :feature_variable "CD123", :final_diagnosis "Ganglioglioma"}
    {:mean 5.27535199210689E-4, :feature_variable "HLADR", :final_diagnosis "Ganglioglioma"}
    {:mean 1.214212253321834E-5, :feature_variable "FoxP3", :final_diagnosis "Ganglioglioma"}
    {:mean 1.1553733134861254E-4, :feature_variable "CD208", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "EphA2", :final_diagnosis "Ganglioglioma"}
    {:mean 1.4668393274053525E-4, :feature_variable "B7H3", :final_diagnosis "Ganglioglioma"}
    {:mean 2.6279630134412384E-4, :feature_variable "ApoE", :final_diagnosis "Ganglioglioma"}
    {:mean 2.6309514252107025E-5,
     :feature_variable "IDO1",
     :final_diagnosis "Diffuse_midline_glioma"}
    {:mean 5.665441830697079E-5, :feature_variable "CD40", :final_diagnosis "Glioma"}
    {:mean 2.507369384634171E-4, :feature_variable "B7H3", :final_diagnosis "Glioma"}
    {:mean 3.809167067538357E-5, :feature_variable "FoxP3", :final_diagnosis "Glioma"}
    {:mean 6.042011457624277E-5, :feature_variable "CD163", :final_diagnosis "Ganglioglioma"}
    {:mean 2.6720363498957024E-4, :feature_variable "CD11b", :final_diagnosis "Ganglioglioma"}
    {:mean 2.4481362780417944E-4, :feature_variable "IDO1", :final_diagnosis "Ganglioglioma"}
    {:mean 1.4273752425713819E-5, :feature_variable "CD206", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "GM2_GD2", :final_diagnosis "Ganglioglioma"}
    {:mean 2.7762674888630007E-5, :feature_variable "CD40", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "TIM3", :final_diagnosis "Ganglioglioma"}
    {:mean 9.914759845343952E-6, :feature_variable "CD133", :final_diagnosis "Ganglioglioma"}
    {:mean 2.760171783726667E-5, :feature_variable "EGFRvIII", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "GM2_GD2", :final_diagnosis "Thalmic_glioma"}
    {:mean 6.905773134275841E-4, :feature_variable "HLA1", :final_diagnosis "Thalmic_glioma"}
    {:mean 0.0, :feature_variable "ICOS", :final_diagnosis "Thalmic_glioma"}
    {:mean 2.55746024388329E-6, :feature_variable "Chym_Tryp", :final_diagnosis "Thalmic_glioma"}
    {:mean 6.232266131390495E-4, :feature_variable "Ki67", :final_diagnosis "Thalmic_glioma"}
    {:mean 5.236643198991909E-4, :feature_variable "GFAP", :final_diagnosis "Ganglioglioma"}
    {:mean 6.609007511607915E-4, :feature_variable "CD45", :final_diagnosis "Ganglioglioma"}
    {:mean 4.1060542142175625E-5, :feature_variable "Olig2", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "LAG3", :final_diagnosis "Ganglioglioma"}
    {:mean 7.69615253521928E-5, :feature_variable "TMEM119", :final_diagnosis "Ganglioglioma"}
    {:mean 1.485166153824333E-4, :feature_variable "H3K27me3", :final_diagnosis "Ganglioglioma"}
    {:mean 4.5072098005494147E-5, :feature_variable "CD47", :final_diagnosis "Ganglioglioma"}
    {:mean 7.217854245208599E-5, :feature_variable "CD141", :final_diagnosis "Ganglioglioma"}
    {:mean 1.3133834908794897E-4, :feature_variable "PDL1", :final_diagnosis "Ganglioglioma"}
    {:mean 1.2683200082093217E-6, :feature_variable "Chym_Tryp", :final_diagnosis "Ganglioglioma"}
    {:mean 7.357959785500618E-5, :feature_variable "HER2", :final_diagnosis "Ganglioglioma"}
    {:mean 1.660527069028238E-4, :feature_variable "Ki67", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "H3K27M", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "GPC2", :final_diagnosis "Ganglioglioma"}
    {:mean 5.916581370313571E-5, :feature_variable "CD86", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "ICOS", :final_diagnosis "Ganglioglioma"}
    {:mean 0.0, :feature_variable "IDO1", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.4147301701553338E-4, :feature_variable "IDH1_R132H", :final_diagnosis "Thalmic_glioma"}
    {:mean 1.7237932396808094E-4, :feature_variable "IDH1_R132H", :final_diagnosis "Ganglioglioma"}
    {:mean 2.7922612729724792E-5, :feature_variable "CD209", :final_diagnosis "Ganglioglioma"}
    {:mean 2.901484157053038E-4, :feature_variable "CD68", :final_diagnosis "Ganglioglioma"}
    {:mean 6.225853139070619E-5, :feature_variable "GLUT1", :final_diagnosis "Ganglioglioma"}))

(defn dendrogram
  [data row-field col-field value-field]
  (let [cluster-l (hd/cluster-data data row-field col-field value-field )
        cluster-u (hd/cluster-data data col-field row-field value-field )]
    [v/vega-view (spec data row-field col-field value-field cluster-l cluster-u) []])
  )

(defn ui
  []
  [:div.p-5
   [dendrogram data1 :final_diagnosis :feature_variable :mean]])
