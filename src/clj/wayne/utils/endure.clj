(ns wayne.utils.endure
  (:require [alandipert.enduro :as e]
            [environ.core :as env]
            [org.candelbio.multitool.core :as u]))

;;; Generalized persistent memoizing based on Enduro https://github.com/alandipert/enduro

;;; Config var ENDURO_MODE can be FILE, MEMORY, OFF, or (eventually) PG

(defn enduro-file
  [name]
  (str ".enduro/" name ".edn"))         ;TODO create dir if necessary

(u/defn-memoized enduro-atom
  [name]
  (case (env/env :enduro-mode)
    "FILE" (e/file-atom {} (enduro-file name) :pending-dir "/tmp")
    ;; TODO for Heroku   :pg
    "MEMORY" (atom {})
    (atom {})
    ))

;;; Endure doesn't work on Heroku (no filesystem). Should use Postgres, but for now, just turn it off.
(defonce unendurable? (= (env/env :enduro-mode) "OFF"))

(defn endure
  "Return a durably memoized version of f, name is the name of the persistance file (under ./.enduro)"
  ([f]
   (endure (str (class f)) f))
  ([name f]
   (let [mem (enduro-atom name)
         swap! (if (instance? alandipert.enduro.EnduroAtom mem) ;let me tell you about these things called methods
                e/swap! swap!)]
     (fn [& args]
       (if unendurable?
         (apply f args)
         (if-let [e (find @mem args)]
           (val e)
           (let [ret (apply f args)]
             (swap! mem assoc args ret) 
             ret)))))))

(defn cached-only
  [name]
  (let [mem (enduro-atom name)]
    (fn [& args]
      (get @mem args))))

(defn cache-read-only
  [name f]
  (let [mem (enduro-atom name)]
    (fn [& args]
      (or (get @mem args)
          (apply f args)))))

;;; TODO clean exceptions, etc
(defn clean
  [name]
  (e/swap! (enduro-atom name) u/clean-map))

(defn forget
  [name & args]
  (e/swap! (enduro-atom name) dissoc args))
