(ns feedn.limit-test
  (:require [clojure.test :refer :all]
            [feedn.limit :as limit]
            [java-time :as jt]))

(deftest test-register-update
  (let [state0 {}
        config {:updates-remaining 2}]
    (testing "on first run, sets up window and decrements updates-remaining"
      (let [state1 (limit/register-update state0 config)]
        (is (contains? state1 :limit-window-start))
        (is (contains? state1 :limit-cutoff-time))
        (is (= (:updates-remaining state1) 1))))
    (testing "between first and last update, decrements updates-remaining and moves cutoff"
      (let [state1 (limit/register-update state0 config)
            _ (Thread/sleep 10)
            state2 (limit/register-update state1 config)]
        (is (= (:updates-remaining state2) 0))
        (is (jt/after? (:limit-cutoff-time state2) (:limit-cutoff-time state1)))))
    (testing "beyond last update, doesn't decrement updates-remaining and doesn't move cutoff"
      (let [state1 (limit/register-update state0 config)
            _ (Thread/sleep 10)
            state2 (limit/register-update state1 config)
            _ (Thread/sleep 10)
            state3 (limit/register-update state2 config)]
        (is (= (:updates-remaining state3) 0))
        (is (= (:limit-cutoff-time state3) (:limit-cutoff-time state2)))))
    (testing "resets updates-remaining when window has closed"
      (let [state1 (limit/register-update state0 config)
            midnight-yesterday (jt/local-date-time
                                 (jt/minus (jt/local-date) (jt/days 1))
                                 (jt/local-time 0))
            state1 (assoc state1 :limit-window-start midnight-yesterday)
            state2 (limit/register-update state1 config)]
        (is (= (:updates-remaining state2) 1))))))
