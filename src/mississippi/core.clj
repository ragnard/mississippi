(ns mississippi.core
  (:require [clojure.walk :as walk]))

(defn add-error [subject attr error]
  (assoc-in subject [:errors attr]
            (conj (get-in subject [:errors attr] []) error)))

(defn safe-parse-int [s]
  (try
    (Integer. s)
    (catch NumberFormatException e
      nil)))

(defn required [subject attr]
  (if (attr subject)
    subject
    (add-error subject attr "required")))

(defn numeric [subject attr]
  (if-let [val (safe-parse-int (attr subject))]
    (assoc subject attr val)
    (add-error subject attr "non numeric")))

(defn member-of
  [lat]
  (fn [subject attr]
    (if (contains? lat (attr subject))
      subject
      (add-error subject
                 attr
                 (format "is not a member of %s"
                         (apply str
                                (interpose ", " lat)))))))

(defn in-range
  [r]
  (fn [subject attr]
    (-> (numeric subject attr) 
        ((member-of (set r)) attr))))

(defn validate
  [subject validations]
  (reduce (fn [acc [attr func]]
            (reduce (fn [acc2 f] (f acc2 attr)) acc func))
          subject validations))

(defn valid? [o]
  (nil? (o :errors)))

(defprotocol Validatable
  (is-valid? [_]))

(defmacro defresource [name & [fields-and-validations]]
  `(defrecord ~name ~(map (comp #'symbol #'name first)
                          fields-and-validations)
     Validatable
     (is-valid? [this#]
                (validate this# ~fields-and-validations))))

;; (macroexpand-1 '(defresource Foo { :a [missisippi.core/numeric] :b [] }))
;; (defresource Foo {:a [(mississippi.core/)]
;;                   :b [mississippi.core/required]})
;; (def f (Foo. "a" nil))
;; (is-valid? f)



