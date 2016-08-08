(ns gilded-rose.core-spec
  (:require [gilded-rose.function-spec :as fs]
            [gilded-rose.core :as gilded]
            [clojure.spec.test :as spec]))

;; see gilden-rose.function-spec and domain-spec
;; for definitions
(println (-> (spec/check)
             spec/summarize-results))
