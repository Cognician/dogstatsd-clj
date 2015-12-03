# Clojure client for DogStatsD, Datadog statsd agent

## Using

Add to project.clj:

```clj
[cognician/dogstatsd-clj "0.1.0"]
```

Require it:

```clj
(require '[cognician.dogstatsd :as d])
```

To configure, provide URL of DogStatsD:

```clj
(d/configure! "localhost:8125")
```

Optionally, you can provide set of global tags to be appended to every metric:

```clj
(d/configure! "localhost:8125" { :tags {:env "production", :project "Secret"} })
```

After that, you can start reporting metrics:

Total value/rate:

```clj
(d/increment! "chat.request.count" 1)
```

In-the-moment value:

```clj
(d/gauge! "chat.ws.connections" 17)
```

Values distribution (mean, avg, max, percentiles):

```clj
(d/histogram! "chat.request.time" 188.17)
```

Counting unique values:

```clj
(d/set! "chat.user.email" "nikita@mailforspam.com")
```

Additional options can be specified as third argument to report functions:

```clj
{ :tags => [String+] | { Keyword -> Any | Nil }
  :sample-rate => Double[0..1] }
```

Tags can be specified as map:

```clj
{:tags { :env "production", :chat nil }} ;; => |#env:production,chat
```

or as a vector:

```clj
{:tags [ "env:production", "chat" ]}     ;; => |#env:production,c3
```

All together:

```clj
(d/configure! "localhost:8125" {:tags {:env "production"}})

(d/increment! "request.count" 1 {:tags ["endpoint:messages__list"]
                                 :sample-rate 0.5})
```
