(ns gilded-rose.function-spec
  (:require [gilded-rose.core :as gilded-core]
            [clojure.spec :as spec]
            [gilded-rose.domain-spec :as domain]))

(spec/fdef gilded-core/minimum-quality-clamp
           :args (spec/cat
                  :quality ::domain/quality)
            :ret ::domain/quality)

(spec/fdef gilded-core/maximum-quality-clamp
           :args (spec/cat
                  :quality ::domain/quality)
           :ret  :common/quality)

(spec/fdef gilded-core/backstage-pass->appreciation
           :args (spec/cat
                  :item ::domain/item)
           :ret (spec/or :sell-in ::domain/sell-in
                         :depleted :depleted/quality-tag))

(spec/fdef gilded-core/backstage-update-operator
           :args (spec/cat
                  :current-amount
                  :common/quality
                  :appreciation
                  (spec/or
                   :depleted :depleted/quality-tag
                   :appreciation (spec/int-in 1 4)))
           :ret (spec/or
                 :depleted :depleted/quality
                 :sell-in ::domain/sell-in)
           :fn (fn [{{curr-amount :current-amount
                      [arg-tag appr] :appreciation} :args
                     [ret-tag sell-in] :ret}]
                 (case [arg-tag ret-tag]
                   ;; depleted should always return depleted
                   [:depleted :depleted] true
                   [:depleted _] false
                   true)))

(spec/fdef gilded-core/default-depreciate-item
           :args (spec/cat
                  :item :depreciative/item-state)
           :ret  :depreciative/item-state
           :fn (fn [{{[arg-tag {sell :sell-in
                                qual :quality}] :item} :args
                     [ret-tag {ret-sell :sell-in
                               ret-qual :quality}] :ret}]
                 (case [arg-tag ret-tag]
                   ;; expired item has quality decremented by two
                   [:expired :expired]  (= 2 (- qual ret-qual))
                   ;; item has quality decremented by one
                   ([:item :depleted]
                    [:item :item]) (= 1 (- qual ret-qual))
                   ;; expired item that depletes to zero is decremented by either
                   ;; 1 or 2 (depending on its distance from zero)
                   [:expired :expired-depleted] (spec/int-in-range? 1 3 (- qual ret-qual))
                   ;; depleted items should have no difference in quality
                   ([:depleted :depleted]
                    [:expired-depleted :expired-depleted]) (zero? (- qual ret-qual))
                   ;; depleted items should only return depleted items
                   ([:expired-depleted _]
                    [:depleted _]) false)))

(spec/fdef gilded-core/depreciate-conjured-item
           :args (spec/cat
                  :item :depreciative/item-state)
           :ret  :depreciative/item-state
           :fn (fn [{{[arg-tag {sell :sell-in
                                qual :quality}] :item} :args
                     [ret-tag {ret-sell :sell-in
                               ret-qual :quality}] :ret}]
                 (case [arg-tag ret-tag]
                   ;; expired conjured item has quality decremented by four
                   [:expired :expired]  (= 4 (- qual ret-qual))
                   ;; conjured item quality decrements by 2 or
                   [:item :item] (= 2 (- qual ret-qual))
                   ;; or 1 if starting with an odd value
                   [:item :depleted] (spec/int-in-range?
                                      1 3
                                      (- qual ret-qual))

                   ;; expired conjured item that depletes to zero is decremented by either
                   ;; 2 or 4 (depending on its distance from zero)
                   [:expired :expired-depleted] (spec/int-in-range? 1 5 (- qual ret-qual))
                   ;; depleted items should have no difference in quality
                   ([:depleted :depleted]
                    [:expired-depleted :expired-depleted]) (zero? (- qual ret-qual))
                   ;; depleted items should only return depleted items
                   ;; expired items should only return expired or expired-depleted items
                   ([:expired-depleted _]
                    [:depleted _]
                    [:expired _]) false)))

(spec/fdef gilded-core/default-appreciate-item
           :args (spec/cat
                  :item :appreciative/item-state)
           :ret  :appreciative/item-state
           :fn (fn [{{[arg-tag {sell :sell-in
                                qual :quality}] :item} :args
                     [ret-tag {ret-sell :sell-in
                               ret-qual :quality}] :ret}]
                 (case [arg-tag ret-tag]

                   ;; general quality increase of one except
                   ([:expired-depleted :expired]
                    [:expired :expired]
                    [:depleted :item]
                    [:item :maxed]
                    [:expired :expired-maxed]
                    [:item :item]) (= 1 (- ret-qual qual))

                   ;; maxed qualities should stay maxed
                   ([:expired-maxed
                     :expired-maxed]
                    [:maxed :maxed]) (zero? (- ret-qual qual))

                   ;; maxed should only return maxed
                   [:maxed  _] false)))

(spec/fdef gilded-core/appreciate-backstage-pass-item
           :args (spec/cat
                  :item :bp-appreciative/item-state)
           :ret  :bp-appreciative/item-state
           :fn (fn [{{[arg-tag {sell :sell-in
                                qual :quality}] :item} :args
                     [ret-tag {ret-sell :sell-in
                               ret-qual :quality}] :ret}]
                 (case [arg-tag ret-tag]

                   ;; expiration -> depletion
                   ([:expired-depleted :expired-depleted]
                    [:expired :expired-depleted]) (zero? ret-qual)

                   ([:item :item]
                    [:depleted :item]
                    [:item :maxed]) (spec/int-in-range? 1 5 (- ret-qual
                                                                qual))
                   ;; maxed qualities should stay maxed
                   ([:maxed :maxed]) (zero? (- ret-qual qual))

                   ;; maxed should only return maxed
                    [:maxed  _]
                     false)))
