;; Copyright © 2016-2017, JUXT LTD.

(ns tick.interval-test
  (:require
   [clojure.spec.alpha :as s]
   [tick.alpha.api :as t]
   #?(:clj
      [clojure.test :refer :all]
      :cljs
      [cljs.test :refer-macros [deftest is testing run-tests]])
   #?(:cljs
      [java.time.temporal :refer [ChronoUnit]])
   [tick.interval :as ti])
  #?(:clj
     (:import
      [java.time.temporal ChronoUnit])))

(s/check-asserts true)

(deftest basic-relations-test
  (is (= (count ti/basic-relations) 13))
  (is (distinct? ti/basic-relations)))

;; We can construct every possible combination of interval relation with just 4 instants.
(def instants [(t/instant "2017-07-30T09:00:00Z")
               (t/instant "2017-07-30T11:00:00Z")
               (t/instant "2017-07-30T13:00:00Z")
               (t/instant "2017-07-30T15:00:00Z")])

;; Distinct: because no pair of definite intervals can be related by more than one of the relationships.
;; From [ALSPAUGH-2009]
(deftest distinct-test
  (is
    (= [1]   ; Each interval should have just one relation that is true
       (distinct
         (let [f (apply juxt ti/basic-relations)]
           (for [x1 instants
                 x2 instants
                 y1 instants
                 y2 instants
                 :when (t/< x1 x2)
                 :when (t/< y1 y2)
                 :let [x (ti/new-interval x1 x2)
                       y (ti/new-interval y1 y2)]]
             ;; For each combination, count how many relations are true
             ;; (should be just one each time)
             (count (filter true? (f x y)))))))))

;; Exhaustive: because any pair of definite intervals are described by one of the relations.
(deftest exhaustive-test []
  (is
    (= 13 ; Thirteen basic relations
       (count
         (distinct
           (for [x1 instants
                 x2 instants
                 y1 instants
                 y2 instants
                 :when (t/< x1 x2)
                 :when (t/< y1 y2)
                 :let [x (ti/new-interval x1 x2)
                       y (ti/new-interval y1 y2)]]
             ;; For each combination, count how many relations are true
             ;; (should be just one each time)
             (ti/relation x y)))))))


(deftest disjoint-test []
  (is (ti/disjoint?
        (ti/new-interval (instants 0) (instants 1))
        (ti/new-interval (instants 2) (instants 3))))
  (is (= (ti/disjoint?
           (ti/new-interval (instants 0) (instants 1))
           (ti/new-interval (instants 2) (instants 3))) ti/precedes?))
  (is (nil?
        (ti/disjoint?
          (ti/new-interval (instants 0) (instants 2))
          (ti/new-interval (instants 1) (instants 3)))))
  (is (nil?
        (ti/disjoint?
          (ti/new-interval (instants 0) (instants 3))
          (ti/new-interval (instants 1) (instants 2))))))

;; concur is really the complement to disjoint, but we'll test it
;; anywhere to ensure the complement function is working as expected.

(deftest concur?-test []
  (is (nil?
        (ti/concur?
          (ti/new-interval (instants 0) (instants 1))
          (ti/new-interval (instants 2) (instants 3)))))
  (is (= (ti/concur?
           (ti/new-interval (instants 0) (instants 2))
           (ti/new-interval (instants 1) (instants 3)))
         ti/overlaps?))
  (is (= (ti/concur?
           (ti/new-interval (instants 0) (instants 3))
           (ti/new-interval (instants 1) (instants 2)))
         ti/contains?)))

(deftest concur-test []
  (is
    (=
      (ti/new-interval (instants 1) (instants 2))
      (ti/concur
        (ti/new-interval (instants 0) (instants 2))
        (ti/new-interval (instants 1) (instants 3)))))

  (is
    (=
      (ti/new-interval (instants 1) (instants 2))
      (ti/concur
        (ti/new-interval (instants 1) (instants 3))
        (ti/new-interval (instants 0) (instants 2)))))

  (is
    (nil?
      (ti/concur
        (ti/new-interval (instants 0) (instants 1))
        (ti/new-interval (instants 2) (instants 3)))))

  (is
    (nil?
      (ti/concur
        (ti/new-interval (instants 0) (instants 1))
        (ti/new-interval (instants 1) (instants 2)))))

  (is
    (=
      (ti/new-interval (instants 0) (instants 2))
      (ti/concur
        (ti/new-interval (instants 0) (instants 2))
        (ti/new-interval (instants 0) (instants 3)))))

  (is
    (=
      (ti/new-interval (instants 0) (instants 2))
      (ti/concur
        (ti/new-interval (instants 0) (instants 3))
        (ti/new-interval (instants 0) (instants 2)))))

  (is
    (=
      (ti/new-interval (instants 1) (instants 3))
      (ti/concur
        (ti/new-interval (instants 1) (instants 3))
        (ti/new-interval (instants 0) (instants 3))))))

;; Sequence tests

;; TODO: Support this: (ti/new-interval (t/now) (t/seconds 10))
;; TODO: Don't allow this: (ti/new-interval (t/now)) -- returns an illegal ti/new-interval

(deftest ordered-disjoint-intervals?-test
  (is
    (ti/ordered-disjoint-intervals? []))
  (is
    (ti/ordered-disjoint-intervals?
      [(ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                        (t/instant "2017-07-30T10:00:00Z"))]))
  (is
    (ti/ordered-disjoint-intervals?
      [(ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                        (t/instant "2017-07-30T10:00:00Z"))
       (ti/new-interval (t/instant "2017-07-30T11:00:00Z")
                        (t/instant "2017-07-30T13:00:00Z"))]))
  (is
    (ti/ordered-disjoint-intervals?
      [(ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                        (t/instant "2017-07-30T11:00:00Z"))
       (ti/new-interval (t/instant "2017-07-30T11:00:00Z")
                        (t/instant "2017-07-30T13:00:00Z"))]))
  (is
    (ti/ordered-disjoint-intervals?
      [(ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                        (t/instant "2017-07-30T11:00:00Z"))
       (ti/new-interval (t/instant "2017-07-30T11:00:00Z")
                        (t/instant "2017-07-30T13:00:00Z"))
       (ti/new-interval (t/instant "2017-07-30T16:00:00Z")
                        (t/instant "2017-07-30T18:00:00Z"))]))
  (is
    (false?
      (ti/ordered-disjoint-intervals?
        [(ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                          (t/instant "2017-07-30T12:00:00Z"))
         (ti/new-interval (t/instant "2017-07-30T11:00:00Z")
                          (t/instant "2017-07-30T13:00:00Z"))])))

  (is
    (false?
      (ti/ordered-disjoint-intervals?
        [(ti/new-interval (t/instant "2017-07-30T11:00:00Z")
                          (t/instant "2017-07-30T13:00:00Z"))
         (ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                          (t/instant "2017-07-30T10:00:00Z"))]))))

(deftest unite-test
  (testing "Unite meeting intervals"
    (is
      (=
        [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))
         (ti/new-interval (t/date "2017-06-26") (t/date "2017-06-30"))]
        (let [intervals
              [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))
               (ti/new-interval (t/date "2017-06-26") (t/date "2017-06-30"))]]
          (ti/unite intervals)))))
  (testing "Unite overlapping intervals"
    (is
      (=
        [(ti/new-interval (t/date "2017-06-10") (t/date "2017-06-25"))
         (ti/new-interval (t/date "2017-07-01") (t/date "2017-07-10"))]

        (map ti/bounds
             (ti/unite [(ti/new-interval (t/date "2017-06-10") (t/date "2017-06-20"))
                        (ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))
                        (ti/new-interval (t/date "2017-07-01") (t/date "2017-07-10"))])))))
  (testing "Unite containing intervals"
    (is
      (=
        [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))]
        (map ti/bounds
             (ti/unite [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))
                        (ti/new-interval (t/date "2017-06-17") (t/date "2017-06-20"))])))))

  (testing "Unite finished-by intervals"
    (is
      (=
        [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))]
        (map ti/bounds
             (ti/unite [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))
                        (ti/new-interval (t/date "2017-06-17") (t/date "2017-06-20"))
                        (ti/new-interval (t/date "2017-06-18") (t/date "2017-06-25"))]))))))

(deftest flatten-test
  (let [ivals [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))
               (ti/new-interval (t/date "2017-06-17") (t/date "2017-06-20"))
               (ti/new-interval (t/date "2017-06-18") (t/date "2017-06-25"))
               (ti/new-interval (t/date "2017-07-18") (t/date "2017-07-25"))
               (ti/new-interval (t/date "2017-08-18") (t/date "2017-08-25"))]]
    (is (= ivals (ti/flatten (ti/unite ivals))))))

(deftest normalize-test
  (let [intervals
        [(ti/new-interval (t/date "2017-06-15") (t/date "2017-06-25"))
         (ti/new-interval (t/date "2017-06-26") (t/date "2017-06-28"))
         (ti/new-interval (t/date "2017-06-30") (t/date "2017-07-04"))]]
    (=
      [{:tick/intervals
        [{:tick/beginning (t/date-time "2017-06-15T00:00")
          :tick/end (t/date-time "2017-06-26T00:00")}
         {:tick/beginning (t/date-time "2017-06-26T00:00")
          :tick/end (t/date-time "2017-06-29T00:00")}]}
       {:tick/intervals
        [{:tick/beginning (t/date-time "2017-06-30T00:00")
          :tick/end (t/date-time "2017-07-05T00:00")}]}]

      (ti/normalize intervals))))

(deftest union-test
  (testing "counts"
    (let [coll1 [(ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                                  (t/instant "2017-07-30T12:00:00Z"))]
          coll2 [(ti/new-interval (t/instant "2017-07-30T11:00:00Z")
                                  (t/instant "2017-07-30T15:00:00Z"))]
          coll3 [(ti/new-interval (t/instant "2017-07-30T17:00:00Z")
                                  (t/instant "2017-07-30T19:00:00Z"))]]
      (is (= 1 (count (ti/union coll1 coll2))))
      (is (ti/ordered-disjoint-intervals? (ti/union coll1 coll2)))
      (is (= 2 (count (ti/union coll1 coll2 coll3))))
      (is (ti/ordered-disjoint-intervals? (ti/union coll1 coll2 coll3)))))

  (testing "union"
    (let [ival1 (ti/new-interval (t/instant "2017-07-30T09:00:00Z")
                                 (t/instant "2017-07-30T10:00:00Z"))
          ival2 (ti/new-interval (t/instant "2017-07-30T10:00:00Z")
                                 (t/instant "2017-07-30T11:00:00Z"))
          ival3 (ti/new-interval (t/instant "2017-07-30T11:00:00Z")
                                 (t/instant "2017-07-30T12:00:00Z"))
          ival4  (ti/new-interval (t/instant "2017-07-30T12:00:00Z")
                                  (t/instant "2017-07-30T13:00:00Z"))
          ival5 (ti/new-interval (t/instant "2017-07-30T13:00:00Z")
                                 (t/instant "2017-07-30T14:00:00Z"))
          res (ti/union [ival2 ival4] [ival1 ival3 ival5])]
      (is (= res [ival1 ival2 ival3 ival4 ival5])))))

(deftest intersection-test
  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T06:00:00Z")
                                (t/instant "2017-01-01T07:00:00Z"))

               (ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T09:00:00Z"))

               (ti/new-interval (t/instant "2017-01-01T09:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))

               (ti/new-interval (t/instant "2017-01-01T13:00:00Z")
                                (t/instant "2017-01-01T15:00:00Z"))

               (ti/new-interval (t/instant "2017-01-01T17:00:00Z")
                                (t/instant "2017-01-01T19:00:00Z"))]

        coll2 [(ti/new-interval (t/instant "2017-01-01T09:00:00Z")
                                (t/instant "2017-01-01T10:00:00Z"))

               (ti/new-interval (t/instant "2017-01-01T11:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))

               (ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                (t/instant "2017-01-01T18:00:00Z"))]]
    (is
      (= [(ti/new-interval (t/instant "2017-01-01T09:00:00Z") (t/instant "2017-01-01T10:00:00Z"))
          (ti/new-interval (t/instant "2017-01-01T11:00:00Z") (t/instant "2017-01-01T12:00:00Z"))
          (ti/new-interval (t/instant "2017-01-01T14:00:00Z") (t/instant "2017-01-01T15:00:00Z"))
          (ti/new-interval (t/instant "2017-01-01T17:00:00Z") (t/instant "2017-01-01T18:00:00Z"))]
         (ti/intersection coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                (t/instant "2017-01-01T16:00:00Z"))]

        coll2 [(ti/new-interval (t/instant "2017-01-01T09:00:00Z")
                                (t/instant "2017-01-01T11:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T13:00:00Z")
                                (t/instant "2017-01-01T17:00:00Z"))]]

    (is
      (= [(ti/new-interval (t/instant "2017-01-01T09:00:00Z") (t/instant "2017-01-01T11:00:00Z"))
          (ti/new-interval (t/instant "2017-01-01T14:00:00Z") (t/instant "2017-01-01T16:00:00Z"))]
         (ti/intersection coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                (t/instant "2017-01-01T16:00:00Z"))]
        coll2 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))]]
    (is
      (=
        [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                          (t/instant "2017-01-01T12:00:00Z"))]
        (ti/intersection coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T17:00:00Z")
                                (t/instant "2017-01-01T19:00:00Z"))]

        coll2 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T18:00:00Z"))]]

    (is (=
          [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                            (t/instant "2017-01-01T12:00:00Z"))
           (ti/new-interval (t/instant "2017-01-01T17:00:00Z")
                            (t/instant "2017-01-01T18:00:00Z"))]
          (ti/intersection coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T12:00:00Z")
                                (t/instant "2017-01-01T14:00:00Z"))]
        coll2 [(ti/new-interval (t/instant "2017-01-01T11:00:00Z")
                                (t/instant "2017-01-01T14:00:00Z"))]]
    (is (= [(ti/new-interval (t/instant "2017-01-01T12:00:00Z")
                             (t/instant "2017-01-01T14:00:00Z"))]
           (ti/intersection coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/parse "2017-04-11T00:00")
                                (t/parse "2017-04-14T00:00"))
               (ti/new-interval (t/parse "2017-04-18T00:00")
                                (t/parse "2017-04-20T00:00"))
               (ti/new-interval (t/parse "2017-12-20T00:00")
                                (t/parse "2017-12-23T00:00"))
               (ti/new-interval (t/parse "2017-12-27T00:00")
                                (t/parse "2018-01-01T00:00"))
               (ti/new-interval (t/parse "2018-01-02T00:00")
                                (t/parse "2018-01-08T00:00"))]
        coll2 [(ti/bounds "2017")]]
    (is (= [(ti/new-interval (t/parse "2017-04-11T00:00")
                             (t/parse "2017-04-14T00:00"))
            (ti/new-interval (t/parse "2017-04-18T00:00")
                             (t/parse "2017-04-20T00:00"))
            (ti/new-interval (t/parse "2017-12-20T00:00")
                             (t/parse "2017-12-23T00:00"))
            (ti/new-interval (t/parse "2017-12-27T00:00")
                             (t/parse "2018-01-01T00:00"))]
           (ti/intersection coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/parse "2017-04-11T00:00")
                                (t/parse "2017-04-14T00:00"))
               (ti/new-interval (t/parse "2017-04-18T00:00")
                                (t/parse "2017-04-20T00:00"))
               (ti/new-interval (t/parse "2017-12-20T00:00")
                                (t/parse "2017-12-23T00:00"))
               (ti/new-interval (t/parse "2017-12-27T00:00")
                                (t/parse "2018-01-01T00:00"))
               (ti/new-interval (t/parse "2018-01-02T00:00")
                                (t/parse "2018-01-08T00:00"))]
        coll2 [(ti/bounds "2017")]]
    (is (= [(ti/new-interval (t/parse "2017-04-11T00:00")
                             (t/parse "2017-04-14T00:00"))
            (ti/new-interval (t/parse "2017-04-18T00:00")
                             (t/parse "2017-04-20T00:00"))
            (ti/new-interval (t/parse "2017-12-20T00:00")
                             (t/parse "2017-12-23T00:00"))
            (ti/new-interval (t/parse "2017-12-27T00:00")
                             (t/parse "2018-01-01T00:00"))]
           (ti/intersection coll1 coll2)))

    (testing "Empty sets"
      (let [coll1 []
            coll2 [(ti/new-interval (t/instant "2017-01-01T09:00:00Z")
                                    (t/instant "2017-01-01T10:00:00Z"))

                   (ti/new-interval (t/instant "2017-01-01T11:00:00Z")
                                    (t/instant "2017-01-01T12:00:00Z"))

                   (ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                    (t/instant "2017-01-01T18:00:00Z"))]]
        (is
          (empty? (ti/intersection coll1 coll2)))
        (is
          (empty? (ti/intersection coll2 coll1)))
        (is
          (empty? (ti/intersection [] [])))))))

(deftest difference-test
  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                (t/instant "2017-01-01T16:00:00Z"))]

        coll2 [(ti/new-interval (t/instant "2017-01-01T09:00:00Z")
                                (t/instant "2017-01-01T11:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T13:00:00Z")
                                (t/instant "2017-01-01T17:00:00Z"))]]

    (is
      (= [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                           (t/instant "2017-01-01T09:00:00Z"))
          (ti/new-interval (t/instant "2017-01-01T11:00:00Z")
                           (t/instant "2017-01-01T12:00:00Z"))]
         (ti/difference coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                (t/instant "2017-01-01T16:00:00Z"))]
        coll2 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))]]
    (is
      (=
        [(ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                          (t/instant "2017-01-01T16:00:00Z"))]
        (ti/difference coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T17:00:00Z")
                                (t/instant "2017-01-01T19:00:00Z"))]


        coll2 [(ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T18:00:00Z"))]]

    (is (=
          [(ti/new-interval (t/instant "2017-01-01T18:00:00Z")
                            (t/instant "2017-01-01T19:00:00Z"))]
          (ti/difference coll1 coll2))))

  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T12:00:00Z")
                                (t/instant "2017-01-01T14:00:00Z"))]
        coll2 [(ti/new-interval (t/instant "2017-01-01T11:00:00Z")
                                (t/instant "2017-01-01T14:00:00Z"))]]
    (is (empty? (ti/difference coll1 coll2))))

  (is (= [(ti/bounds "2017-07-31" "2017-08-13")]
         (ti/difference
           [(ti/bounds "2017-07-31" "2017-08-13")]
           [(ti/bounds "2017-01-01")])))

  (testing "Empty sets"
    (let [coll1 []
          coll2 [(ti/new-interval (t/instant "2017-01-01T09:00:00Z")
                                  (t/instant "2017-01-01T10:00:00Z"))

                 (ti/new-interval (t/instant "2017-01-01T11:00:00Z")
                                  (t/instant "2017-01-01T12:00:00Z"))

                 (ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                  (t/instant "2017-01-01T18:00:00Z"))]]
      (is
        (= []
           (ti/difference coll1 coll2)))
      (is
        (= coll2
           (ti/difference coll2 coll1)))
      (is
        (= []
           (ti/difference [] []))))))

(deftest difference-invariant-test
  (let [coll1 [(ti/new-interval (t/instant "2017-01-01T14:00:00Z")
                                (t/instant "2017-01-01T16:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T08:00:00Z")
                                (t/instant "2017-01-01T12:00:00Z"))]

        coll2 [(ti/new-interval (t/instant "2017-01-01T09:00:00Z")
                                (t/instant "2017-01-01T11:00:00Z"))
               (ti/new-interval (t/instant "2017-01-01T13:00:00Z")
                                (t/instant "2017-01-01T17:00:00Z"))]]
    (is
      (thrown?
        #?(:clj clojure.lang.ExceptionInfo
           :cljs ExceptionInfo)
        (ti/difference coll1 coll2)))))

;; We are reclaiming 'disjoin' to mean to 'end the joining of' or 'to become disjoint'.

#_(deftest disj-test
    (is (=
          [(ti/new-interval (t/date-time "2017-01-01T00:00")
                            (t/date-time "2017-07-04T00:00"))
           (ti/new-interval (t/date-time "2017-07-05T00:00")
                            (t/date-time "2018-01-01T00:00"))]
          (disj [(bounds "2017")] (bounds (t/date "2017-07-04"))))))

(deftest complement-test
  (testing "complement through max of type"
    (is (= [(ti/new-interval (t/time "01:00") (t/max-of-type (t/time "00:00")))]
           (ti/complement [(ti/new-interval (t/time "00:00") (t/time "01:00"))]))))
  (testing "complement ordered disjoint intervals"
    (is (= [(ti/new-interval (t/time "00:00") (t/time "01:00"))
            (ti/new-interval (t/time "02:00") (t/time "03:00"))
            (ti/new-interval (t/time "04:00") (t/max-of-type (t/time "00:00")))]
           (ti/complement [(ti/new-interval (t/time "01:00") (t/time "02:00"))
                        (ti/new-interval (t/time "03:00") (t/time "04:00"))]))))
  (testing "complement meeting intervals"
    (is (= [(ti/new-interval (t/time "00:00") (t/time "01:00"))
            (ti/new-interval (t/time "03:00") (t/max-of-type (t/time "00:00")))]
           (ti/complement [(ti/new-interval (t/time "01:00") (t/time "02:00"))
                        (ti/new-interval (t/time "02:00") (t/time "03:00"))]))))
  (testing "complement empty interval round trip"
    (is (= [] (ti/complement (ti/complement []))))))


;; Division test

(deftest division-test
  (is (= 7 (count (t/divide (ti/bounds (t/year 2012) (t/year 2018)) t/year)))))

(deftest group-by-intervals-test
  (testing "p and s"
    (t/with-clock t/UTC
      (is
        (=
          {(t/year 2017) [(ti/new-interval
                            (t/date-time "2017-12-20T00:00")
                            (t/date-time "2018-01-01T00:00"))]
           (t/year 2018) [(ti/new-interval
                            (t/date-time "2018-01-01T00:00")
                            (t/date-time "2018-01-10T00:00"))]}
          (ti/group-by
            (t/divide (ti/bounds (t/year 2016) (t/year 2019)) t/year)
            [(ti/new-interval (t/date-time #inst "2017-12-20")
               (t/date-time #inst "2018-01-10"))])))))

  (testing "O"
    (is
      (=
        {(t/year 2015) [(ti/bounds (t/year-month "2015-06") (t/year-month "2015-12"))]
         (t/year 2016) [(ti/bounds (t/year 2016))]
         (t/year 2017) [(ti/bounds (t/year-month "2017-01") (t/year-month "2017-06"))]}
        (ti/group-by
          (t/divide (ti/bounds (t/year 2014) (t/year 2018)) t/year)
          [(ti/bounds (t/year-month "2015-06") (t/year-month "2017-06"))]))))

  (testing "M and e"
    (is
      (=
        {(t/year 2015) [(t/year 2015)]
         (t/year 2016) [(t/year 2016)]}
        (ti/group-by
          (t/divide (ti/bounds (t/year 2014) (t/year 2017)) t/year)
          (t/divide (ti/bounds (t/year 2015) (t/year 2016)) t/year)))))

  (testing "s"
    (is (=
          {(t/year 2015) [(ti/bounds (t/year 2015))]
           (t/year 2016) [(ti/bounds (t/year 2016))]}
          (ti/group-by
            (t/divide (ti/bounds (t/year 2014) (t/year 2017)) t/year)
            [(ti/bounds (t/year 2015) (t/year 2016))]))))

  (testing "f"
    (is
      (=
        {(t/year 2015) [(ti/bounds (t/year-month "2015-06") (t/year-month "2015-12"))]}
        (ti/group-by
          [(t/year 2014) (t/year 2015) (t/year 2016)]
          [(ti/bounds (t/year-month "2015-06") (t/year-month "2015-12"))]))))

  (testing "F"
    (is
      (=
        {(ti/bounds (t/year-month "2015-06") (t/year-month "2015-12"))
         [(ti/bounds (t/year-month "2015-06") (t/year-month "2015-12"))]}
        (ti/group-by
          [(ti/bounds (t/year-month "2015-06") (t/year-month "2015-12"))]
          [(t/year 2014) (t/year 2015) (t/year 2016)]))))

  (testing "d"
    (is
      (=
        {(t/year 2015) [(ti/bounds (t/year-month "2015-03") (t/year-month "2015-09"))]}
        (ti/group-by
          [(t/year 2014) (t/year 2015) (t/year 2016)]
          [(ti/bounds (t/year-month "2015-03") (t/year-month "2015-09"))]))))

  (testing "D"
    (is
      (=
        {(ti/bounds (t/year-month "2015-03") (t/year-month "2015-09"))
         [(ti/bounds (t/year-month "2015-03") (t/year-month "2015-09"))]}

        (ti/group-by
          [(ti/bounds (t/year-month "2015-03") (t/year-month "2015-09"))]
          [(t/year 2014) (t/year 2015) (t/year 2016)]))))

  (testing "o"
    (is
      (=
        {(ti/bounds (t/year-month "2015-06") (t/year-month "2017-06"))
         [(ti/bounds (t/year-month "2015-06") (t/year-month "2015-12"))
          (t/year "2016")
          (ti/bounds (t/year-month "2017-01") (t/year-month "2017-06"))]}
        (ti/group-by
          [(ti/bounds (t/year-month "2015-06") (t/year-month "2017-06"))]
          (t/divide (ti/bounds (t/year 2014) (t/year 2018)) t/year))))))

(deftest group-by-test
  (is
    (= 31
       (count
         (ti/group-by
           t/date
           [(t/date "2015-05-20") (t/year-month "2015-06")])))))

(deftest group-by-empty-test
  (is (= {} (ti/group-by t/date []))))
