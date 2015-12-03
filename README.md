# Clojure client for DogStatsD, Datadog’s StatsD agent

## Setting things up

Add to project.clj:

```clj
[cognician/dogstatsd-clj "0.1.0"]
```

Require it:

```clj
(require '[cognician.dogstatsd :as d])
```


## Configuring

To configure, provide URL of DogStatsD:

```clj
(d/configure! "localhost:8125")
```

Optionally, you can provide set of global tags to be appended to every metric:

```clj
(d/configure! "localhost:8125" { :tags {:env "production", :project "Secret"} })
```


## Reporting

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

To measure function execution time, use `d/measure!`:

```clj
(d/measure! "thread.sleep.time" {}
  (Thread/sleep 1000))
```

Counting unique values:

```clj
(d/set! "chat.user.email" "nikita@mailforspam.com")
```


## Tags and throttling

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
{:tags [ "env:production", "chat" ]}     ;; => |#env:production,chat
```


## Events:

```clj
(d/event! "title" "text" opts)
```

where opts could contain any subset of:

```clj
{ :tags             => [String+] | { Keyword -> Any | Nil }
  :date-happened    => java.util.Date
  :hostname         => String
  :aggregation-key  => String
  :priority         => :normal | :low
  :source-type=name => String
  :alert-type       => :error | :warning | :info | :success }
```


## Example

```clj
(require '[cognician/dogstatsd :as d])

(d/configure! "localhost:8125" {:tags {:env "production"}})

(d/increment! "request.count" 1 {:tags ["endpoint:messages__list"]
                                 :sample-rate 0.5})
```


## License

Copyright © 2015 Cognician Software (Pty) Ltd

Distributed under the Eclipse Public License, the same as Clojure.
