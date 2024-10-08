(ns wayne.team
  (:require
   [wayne.templating :as template]
   [clojure.java.io :as io]
   )
  )

;;; Uses the template system but in a different way.
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

;;; ??? 

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
    :link "https://profiles.stanford.edu/benjamin-oberlton"
    :title "PhD Graduate Student, Stanford University"
    }
   ]
  )

(defn team-cards
  []
  (let [template (slurp (io/resource "templates/components/team-member-card.html"))]
    (apply str
           (map (partial template/expand-template template)
                team))))
