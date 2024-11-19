(ns wayne.team
  (:require
   [org.candelbio.multitool.core :as u]
   [clojure.java.io :as io]
   )
  )

;;; Uses the template system but in a different awkward way.
;;; Writes back into templates/components directory so result can be put into page. Kludgy.

;;; Note: :link2 not yet used in th4e template, not sure what to do with it
;;; Note: :instagram and :facebook also supported, but no data yet

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

(def collaborators
  [
   {:name "Crystal Mackall",
    :image "https://med.stanford.edu/services/api/cap/profiles/photocache.73276.jpg"
    :link "https://med.stanford.edu/mackalllab/People.html"
    :link2 "https://med.stanford.edu/profiles/crystal-mackall"
    :title "Professor, Stanford University"
    :twitter "https://x.com/mackalllab/"
    }

   {:name "Robert M. Prins"
    :title "Professor, Neurosurgery and Pharmacology, David Geffen School of Medicine at UCLA"
    :link "https://robertprinslab.healthsciences.ucla.edu/"
    :image "https://robertprinslab.healthsciences.ucla.edu/sites/g/files/oketem1351/files/styles/1_1_960px/public/media/images/prins-robert.jpg.webp"
    :linkedin "https://www.linkedin.com/in/robert-prins-50a62743/"
    :twitter "https://x.com/UCLANsgy/"
    :link2 "https://bri.ucla.edu/people/robert-prins/"
    }

   {:name "Hideho Okada",
    :image "https://braintumorcenter.ucsf.edu/sites/default/files/styles/person/public/2018-04/okada_hideho_740x864.jpg"
    :link "https://okadalab.ucsf.edu/"
    :link2 "https://profiles.ucsf.edu/hideho.okada"
    :twitter "https://twitter.com/okadalabucsf1"
    :title "Professor, University of California, San Francisco"
    }

   {:name "Christine Brown",
    :image "https://www.cityofhope.org/sites/www/files/styles/small_bio_portrait_315x450_/public/image/christine-brown.jpg"
    :link "https://www.cityofhope.org/research/beckman-research-institute/immuno-oncology/christine-brown-lab"
    :link2 "https://www.cityofhope.org/christine-brown"
    :title "Professor, City of Hope"
    }

   {:name "Kristina Cole",
    :image "https://www.chop.edu/sites/default/files/styles/crop_and_center_300x375/public/providers-new-cole-kristina.jpg"
    :link "https://www.chop.edu/doctors/cole-kristina-a"
    :title "Associate Professor, Children's Hospital of Philadelphia"
    }

   {:name "Derek Oldridge",
    :image "https://pathology.med.upenn.edu/sites/default/files/styles/person/public/oldridge_derek_20232.jpg"
    :link "https://www.theoldridgelab.com/"
    :link2 "https://pathology.med.upenn.edu/department/people/1699/derek-alan-oldridge"
    :title "Assistant Professor, Children's Hospital of Philadelphia"
    :twitter "https://twitter.com/oldridgederek"
    }



   {:name "Joanna J. Phillips"
    :title "Professor, Brain Tumor Center, UCSF"
    :link "https://phillipslab.ucsf.edu/"
    :image "https://phillipslab.ucsf.edu/sites/default/files/styles/person/public/2020-01/PhillipsJ_Headshot_20160921_0072_740x864.jpg"
    }

   {:name "EnJun Yang"
    :title "Research Director, PICI"
    :image "https://www.parkerici.org/wp-content/uploads/EnJun-Yang-PhD-1.jpeg"
    :link "https://www.parkerici.org/person/enjun-yang-phd/"
    :linkedin "https://www.linkedin.com/in/enjun-yang-82689638/"
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

(defn generate-1
  [list output]
  (let [template (slurp (io/resource "templates/components/team-member-card.html"))]
    (spit (str "resources/templates/components/" output)
          (apply str
           (map (partial expand-template template)
                list)))))

(defn generate
  []
  (generate-1 team "team-cards.html")
  (generate-1 collaborators "collaborator-cards.html"))


