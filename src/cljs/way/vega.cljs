(ns way.vega
  )

;;; Spec is vega spec with data
;;; dom-id is a dom specifier like "#viz"
(defn do-vega
  [spec dom-id]
  (js/module$node_modules$vega_embed$build$vega_embed.embed dom-id (clj->js spec)))


