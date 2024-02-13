(ns way.vega
  )

;;; This is a very non-react way to do things. TODO make ir more react to make hot-reloading easier

;;; Spec is vega spec with data
;;; dom-id is a dom specifier like "#viz"
(defn do-vega
  [spec dom-id]
  (js/module$node_modules$vega_embed$build$vega_embed.embed dom-id (clj->js spec)))


