(ns gilded-rose.domain-spec
  (:require [clojure.spec :as spec]
            [clojure.string :as str]
            [clojure.test.check.generators :as check-gen]))

;; core item attributes
(spec/def ::name string?)
(spec/def ::sell-in int?)
(spec/def ::quality nat-int?)

;; basic item spec
(spec/def ::item (spec/keys :req-un [::name ::sell-in ::quality]))

;; legendary
(spec/def :legendary/quality (partial = 80))

(spec/def :legendary/name #(str/includes? % "Sulfuras"))

(spec/def :legendary/item
  (spec/and ::item
            (spec/keys :req-un [:legendary/quality
                                :legendary/name])))

;; non-legendary, or common
(spec/def :common/quality (spec/int-in 0 51))
(spec/def :common/item (spec/keys :req-un [:common/quality]))


;; appreciative
(spec/def :backstage-pass/name #(str/includes? % "Backstage passes"))

(spec/def :aged-brie/name #(str/includes? % "Aged Brie"))

(spec/def :appreciative/item (spec/and ::item
                                         :common/item
                                         (spec/or ::backstage-pass
                                                  (spec/keys :req-un [:backstage-pass/name])
                                                  ::aged-brie
                                                  (spec/keys :req-un [:aged-brie/name]))))

;; depreciative
(spec/def :conjured/name #(str/includes? % "Conjured"))

(spec/def :depreciative/item (spec/and ::item
                                         :common/item
                                         (spec/or
                                          ::conjured (spec/keys :req-un [:conjured/name])
                                          ::default-item  identity)))

;; item states :: depleted or expired or maxed or expired-depleted

;; maxed item
(spec/def :maxed/quality (spec/with-gen (spec/and int? #(= 50 %))
                           (constantly (check-gen/return 50))))

(spec/def :positive/sell-in nat-int?)

(spec/def :maxed/item (spec/keys :req-un [::name
                                          :maxed/quality
                                          :positive/sell-in]))

;; depleted item

(spec/def :depleted/quality-tag (spec/with-gen #(= % :depleted/quality)
                                  (constantly (check-gen/return
                                               :depleted/quality))))

(spec/def :depleted/quality zero?)

(spec/def :expired/sell-in (spec/and int? neg?))

(spec/def :expired/item (spec/merge ::item
                                    :common/item
                                    (spec/keys :req-un [:expired/sell-in])))

(spec/def :depleted/item (spec/keys :req-un [::name ::sell-in :depleted/quality]))

;; valid states for each group of items

(spec/def :depreciative/item-state
  (spec/or
   :expired-depleted (spec/keys :req-un [::name :expired/sell-in :depleted/quality])
   :expired :expired/item
   :depleted :depleted/item
   :item (spec/merge :common/item ::item)))

(spec/def :appreciative/item-state
  (spec/or
   :expired-depleted (spec/keys :req-un [::name :expired/sell-in :depleted/quality])
   :expired-maxed (spec/keys :req-un [::name :expired/sell-in :maxed/quality])
   :maxed (spec/keys :req-un [::name :positive/sell-in :maxed/quality])
   :expired :expired/item
   :depleted :depleted/item
   :item (spec/merge :common/item ::item)))

(spec/def :bp-appreciative/item-state
  (spec/or
   :expired-depleted (spec/keys :req-un [::name :expired/sell-in :depleted/quality])
   :maxed (spec/keys :req-un [::name :positive/sell-in :maxed/quality])
   :expired :expired/item
   :depleted :depleted/item
   :item (spec/merge :common/item ::item)))


(def item-spec
  (spec/or
   ;; timeless
   ::legendary :legendary/item
   ::appreciative :appreciative/item
   ;; items that depreciate in value over time
   ::depreciative :depreciative/item))

(def item-conform (partial spec/conform item-spec))
