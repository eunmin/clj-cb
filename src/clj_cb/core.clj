(ns clj-cb.core
  (:import [com.netflix.hystrix.util HystrixRollingNumber HystrixRollingNumberEvent]))

(def ^:private success-event HystrixRollingNumberEvent/SUCCESS)
(def ^:private failure-event HystrixRollingNumberEvent/FAILURE)

(defn make-circuit-breaker [{:keys [time-in-milliseconds
                                    number-of-buckets
                                    sleep-window-in-milliseconds
                                    request-volume-threshold
                                    error-threshold-percentage]}]
  {:status (atom :close)
   :circuit-opened (atom -1)
   :error-threshold-percentage error-threshold-percentage
   :sleep-window-in-milliseconds sleep-window-in-milliseconds
   :request-volume-threshold request-volume-threshold
   :metrics (HystrixRollingNumber. time-in-milliseconds number-of-buckets)})

(defn- after-sleep-window? [{:keys [circuit-opened sleep-window-in-milliseconds]}]
  (> (System/currentTimeMillis)
     (+ @circuit-opened sleep-window-in-milliseconds)))

(defn attempt-execution? [{:keys [status circuit-opened] :as cb}]
  (or (neg? @circuit-opened)
      (and (after-sleep-window? cb)
           (compare-and-set! status :open :half-open))))

(defn success! [{:keys [status circuit-opened ^HystrixRollingNumber metrics]}]
  (.increment metrics success-event)
  (when (compare-and-set! status :half-open :close)
    (.reset metrics)
    (reset! circuit-opened -1)))

(defn- open? [{:keys [^HystrixRollingNumber metrics
                      request-volume-threshold
                      error-threshold-percentage]}]
  (let [success (.getRollingSum metrics success-event)
        failure (.getRollingSum metrics failure-event)
        total (+ success failure)]
    (and (> total request-volume-threshold)
         (> (* (float (/ failure total)) 100)
            error-threshold-percentage))))

(defn failure! [{:keys [status circuit-opened ^HystrixRollingNumber metrics] :as cb}]
  (.increment metrics failure-event)
  (when (compare-and-set! status :half-open :open)
    (reset! circuit-opened (System/currentTimeMillis)))
  (when (and (open? cb) (compare-and-set! status :close :open))
    (reset! circuit-opened (System/currentTimeMillis))))
