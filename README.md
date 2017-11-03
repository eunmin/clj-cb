# clj-cb

Simple Circuit Breaker using Hystrix

## Usage

```clojure
(def cb (make-circuit-breaker {:time-in-milliseconds 10000
                               :number-of-buckets 10
                               :sleep-window-in-milliseconds 5000
                               :request-volume-threshold 20
                               :error-threshold-percentage 50}))

(when (attempt-execution? cb)
   (if (do-something)
     (cb/success! cb)
     (cb/failure! cb)))
```


## License

Copyright © 2017 Eunmin Kim

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
