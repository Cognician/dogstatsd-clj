# Clojure client for DogStatsD, Datadog’s StatsD agent

For general information about DataDog, DogStatsD, how they're useful
and why all this is useful - read the _Rationale, Context, additional
documentation_ section below.

## Setting things up

Add to project.clj:

```clj
[cognician/dogstatsd-clj "0.1.1"]
```

Require it:

```clj
(require '[cognician.dogstatsd :as dogstatsd])
```


## Configuring

To configure, provide URL of DogStatsD:

```clj
(dogstatsd/configure! "localhost:8125")
```

Optionally, you can provide set of global tags to be appended to every metric:

```clj
(dogstatsd/configure! "localhost:8125" { :tags {:env "production", :project "Secret"} })
```


## Reporting

After that, you can start reporting metrics:

Total value/rate:

```clj
(dogstatsd/increment! "chat.request.count" 1)
```

In-the-moment value:

```clj
(dogstatsd/gauge! "chat.ws.connections" 17)
```

Values distribution (mean, avg, max, percentiles):

```clj
(dogstatsd/histogram! "chat.request.time" 188.17)
```

To measure function execution time, use `d/measure!`:

```clj
(dogstatsd/measure! "thread.sleep.time" {}
  (Thread/sleep 1000))
```

Counting unique values:

```clj
(dogstatsd/set! "chat.user.email" "nikita@mailforspam.com")
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
(dogstatsd/event! "title" "text" opts)
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
(require '[cognician/dogstatsd :as dogstatsd])

(dogstatsd/configure! "localhost:8125" {:tags {:env "production"}})

(dogstatsd/increment! "request.count" 1 {:tags ["endpoint:messages__list"]
                                 :sample-rate 0.5})
```

## Rationale, context, additional documentation ##

### Rationale and Context ###
DataDog, being a monitoring service, has the ability, through their
DogStatsD implementation, to collect and show important information
like when things are happening, and how long those things take.

An example is here:
[Cog Validation
Time](https://app.datadoghq.com/dash/211555/production-monolith?screenId=211555&screenName=production-monolith&from_ts=1544104800000&is_auto=false&live=true&page=0&to_ts=1544191200000&fullscreen_widget=399429687&tile_size=m)

(It should show how long a validation function takes, which, over
time, we hope to correlate with core dumps or slow service events)

Because the data is pulled into DataDog, the graph widgets can be
pulled into dashboards, so synchronisation and correlation can take
place.

### Local testing ###

Since DogStatsD is DataDog's service, you'll want to tighten the loop
on feedback and prevent contamination of production data with
dev/testing info.

An excellent package is
[https://github.com/jonmorehouse/dogstatsd-local](https://github.com/jonmorehouse/dogstatsd-local) 

It allows you to create a StatsD listener on localhost - and spits
results out in the terminal when you make calls. The process is pretty
straightforward:
- Install go `brew install go` worked nicely enough
- Clone the repository: `git clone https://github.com/jonmorehouse/dogstatsd-local.git` 
- cd into the repository and enter `go build`
- then, run `./dogstatsd-local -port=8126`

dogstatsd-local is now listening on port 8126.

You'll need to then tell whatever is using this library at
configure-time to send requests to localhost:8126.

To make this work for Manage, the following was
added to the ~/.zshrc file:

`export COGNICIAN_STATSD_URI="localhost:8126"`

... then, when manage runs, it uses the configuration library, which
ultimately reads this value from the system environment.

Now, when manage is run in dev mode and instrumented code is hit, the results are
available immediately in the terminal :)

### Conventions ###
Of course, you can do whatever you want, but it's much more convenient
for everyone if you include it as "dogstatsd" - so searching across
codebases is easier ;)

## CHANGES

*0.1.2*

- Remove reflection warnings

*0.1.1*

- Metric reporting methods now catch all errors, print them to stderr and continue

*0.1.0*

- Initial release

## License

Copyright © 2015 Cognician Software (Pty) Ltd

Distributed under the Eclipse Public License, the same as Clojure.
