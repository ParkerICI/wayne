(ns wayne.team
  (:require
   [org.candelbio.multitool.core :as u]
   [clojure.java.io :as io]
   )
  )

;;; Uses the template system but in a different awkward way.
;;; Writes back into templates/components directory so result can be put into page. Kludgy.

;;; TODO need to make twitter icons etc conditional somehow. Bleah


(def team
  [
   {:name "Mike Angelo, MD PhD",
    :image "https://static.wixstatic.com/media/302cbc_c48a81748a00498e82338f6a9dc430bd~mv2.png/v1/fill/w_566,h_566,al_c,q_85,usm_0.66_1.00_0.01,enc_auto/angelo.png"
    :link "https://www.angelolab.com/people"
    :link2 "https://med.stanford.edu/profiles/robert-angelo"
    :title "Principle Investigator, Associate Professor, Stanford University"
    :twitter "https://x.com/mikeangelolab"
    }

   {:name "Sean Bendall, PhD"
    :image "https://bendall-lab.stanford.edu/people/profiles/_bendall.jpg"
    :link "https://bendall-lab.stanford.edu/"
    :link2 "https://profiles.stanford.edu/sean-bendall"
    :twitter "https://x.com/bendall_lab"
    :title "Principle Investigator, Associate Professor, Stanford University"
    }

   {:name "Hadeesha Piyadasa, PhD"
    :image "https://bendall-lab.stanford.edu/people/profiles/_piyadasa.jpg"
    :link "https://profiles.stanford.edu/hadeesha-piyadasa"
    :title "Postdoctoral Fellow, Stanford University"
    :twitter "https://x.com/hadeeshap"
    :linkedin "www.linkedin.com/in/hpiyadasa"
    }

   {:name "Benjamin Oberlton"
    :image "https://med.stanford.edu/immunol/news/2020/Stanford-Immunology-Welcomes-New-PhD-Students/_jcr_content/main/panel_builder/panel_2/image.img.476.high.jpg/Ben-Oberlton_photo.JPG"
    :link "https://profiles.stanford.edu/benjamin-oberlton"
    :title "PhD Graduate Student, Stanford University"
    }
   ]
  )

;;; Dupe from templating because namespace issues
(defn expand-template
  [template params]
  (u/expand-template template
                     params
                     :param-regex u/double-braces
                     :key-fn keyword))

(defn generate
  []
  (let [template (slurp (io/resource "templates/components/team-member-card.html"))]
    (spit "resources/templates/components/team-cards.html"
          (apply str
           (map (partial expand-template template)
                team)))))
