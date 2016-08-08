(ns gilded-rose.core
  (:require [clojure.spec :as spec]
            [gilded-rose.domain-spec :as domain]

            [clojure.string :as str]))

(defn assess-dispatch
  "Takes an items conformed value and returns a vector
  of descriptors to dispatch on."
  [conformed-item]
  (->> conformed-item
       flatten
       (take-while keyword?)
       vec))

(defn just-item
  [conformed-item]
  (->> conformed-item
       flatten
       (drop-while keyword?)
       first))

(def minimum-quality-clamp
  (partial max 0))

(def maximum-quality-clamp
  (partial min 50))

(defn update-item-quality
  "High order function used as a basis for dispatched
  items to have their quality update logic implemented."
  [amount-fn
   clamp-fn
   operator-fn
   item]
  (let [amount (amount-fn item)
        update-with-clamp (comp
                           clamp-fn
                           #(operator-fn
                             %
                             amount))]
    (update item :quality update-with-clamp)))


(defn assess-with
  "Updates item within the contexted of its conformed form."
  [item-assesser conformed-item]
  (update-in conformed-item [1 1] item-assesser))

(defn sell-in->depreciation
  [sell-in]
  (if (neg? sell-in)
    2
    1))

(def item->depreciation
  (comp
   sell-in->depreciation
   :sell-in))

(def conjured-item->depreciation
  (comp (partial * 2)
        item->depreciation))

(defn backstage-pass->appreciation
  [{:keys [sell-in] :as item}]
  (cond
    (neg? sell-in) :depleted/quality
    (<= sell-in 5)  3
    (<= sell-in 10) 2
    :else 1))

(defn backstage-update-operator
  "A backstage pass' quality switches to zero
  after the event has passed, else it will
  be added to the current quality."
  [current-amount appreciation]
  (if (= appreciation :depleted/quality)
    0
    (+ current-amount appreciation)))


(def default-depreciate-item
  (partial update-item-quality
           item->depreciation
           minimum-quality-clamp
           -))

(def depreciate-conjured-item
  (partial update-item-quality
           conjured-item->depreciation
           minimum-quality-clamp
           -))

(def default-appreciate-item
  (partial update-item-quality
           (constantly 1)
           maximum-quality-clamp
           +))

(def appreciate-backstage-pass-item
  (partial update-item-quality
           backstage-pass->appreciation
           maximum-quality-clamp
           backstage-update-operator))

(defn tick-back-day
  "Decrements items sell-in value"
  [item]
  (update item :sell-in dec))

(defmulti assess-item assess-dispatch)

(defmethod assess-item [::domain/legendary]
  [conformed-item]
  (update-in conformed-item [1] tick-back-day))

(defmethod assess-item [::domain/depreciative ::domain/default-item]
  [[_ [depreciative-kind item] :as conformed-item]]
  (assess-with
   (comp default-depreciate-item tick-back-day)
   conformed-item))

(defmethod assess-item [::domain/depreciative ::domain/conjured]
  [[_ [depreciative-kind item] :as conformed-item]]
  (assess-with
   (comp depreciate-conjured-item tick-back-day)
   conformed-item))

(defmethod assess-item [::domain/appreciative ::domain/aged-brie]
  [[_ [depreciative-kind item] :as conformed-item]]
  (assess-with
   (comp  default-appreciate-item tick-back-day)
   conformed-item))

(defmethod assess-item [::domain/appreciative ::domain/backstage-pass]
  [[_ [depreciative-kind item] :as conformed-item]]
  (assess-with
   (comp appreciate-backstage-pass-item tick-back-day)
   conformed-item))

(defn item [item-name, sell-in, quality]
  {:name item-name, :sell-in sell-in, :quality quality})

(defn update-current-inventory[]
  (let [inventory
        [
         (item "+5 Dexterity Vest" 10 20)
         (item "Aged Brie" 2 0)
         (item "Elixir of the Mongoose" 5 7)
         (item "Sulfuras, Hand Of Ragnaros" 0 80)
         (item "Backstage passes to a TAFKAL80ETC concert" 15 20)
         (item "Conjured Mana Cake" 3 6)
         ]
        conformed-inventory (map domain/item-conform inventory)]
    (map (comp just-item assess-item) conformed-inventory)))
