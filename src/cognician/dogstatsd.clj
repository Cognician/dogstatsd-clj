(ns ^{:doc
"    (configure! \"localhost:8125\")
     
     Total value/rate:
      
       (increment! \"chat.request.count\"  1)
      
     In-the-moment value:
      
       (gauge!     \"chat.ws.connections\" 17)
      
     Values distribution (mean, avg, max, percentiles):
      
       (histogram! \"chat.request.time\"   188.17)
      
     Counting unique values:
      
       (set!       \"chat.user.email\"   \"nikita@mailforspam.com\")
      
     Supported opts (third argument):
      
       { :tags => [String+] | { Keyword -> Any | Nil }
         :sample-rate => Double[0..1] }
      
     E.g. (increment! \"chat.request.count\" 1
            { :tags        { :env \"production\", :chat nil } ;; => |#env:production,chat
              :tags        [ \"env:production\"  \"chat\" ]   ;; => |#env:production,chat
              :sample-rate 0.5 }                              ;; Throttling 50%"}
  cognician.dogstatsd
  (:require
    [clojure.string :as str])
  (:import
    [java.net InetSocketAddress DatagramSocket DatagramPacket]))


(defonce *state (atom nil))


(defn configure!
  "Just pass StatsD server URI:
  
     (configure! \"localhost:8125\")
     (configure! \":8125\")
     (configure! \"localhost\")
    
   Pass system-wide tags to opts:
  
     (configure! \"localhost:8125\" {:tags {:env \"production\"}})"
  ([uri] (configure! uri {}))
  ([uri opts]
    (when-let [[_ host port] (and uri (re-matches #"([^:]*)(?:\:(\d+))?" uri))]
      (let [host   (if (str/blank? host) "localhost" host)
            port   (if (str/blank? port) 8125 port)
            port   (if (string? port) (Integer/parseInt port) port)]
        (reset! *state (merge (select-keys opts [:tags])
                         { :socket (DatagramSocket.)
                           :addr   (InetSocketAddress. host port) }))))))


(defn- send! [payload]
;;   (println "[ metrics ]" payload)
  (if-let [{:keys [socket addr]} @*state]
    (let [bytes (.getBytes payload "UTF-8")]
      (try
        (.send ^DatagramSocket socket
               (DatagramPacket. bytes (alength bytes) ^InetSocketAddress addr))
        (catch Exception e
          (.printStackTrace e))))))


(defn- format-tags [& tag-colls]
  (->> tag-colls
    (mapcat (fn [tags]
              (cond->> tags
                (map? tags) (map (fn [[k v]]
                                   (if (nil? v)
                                     (name k)
                                     (str (name k) ":" v)))))))
    (str/join ",")))


(defn- format-metric [metric type value tags sample-rate]
  (assert (re-matches #"[a-zA-Z][a-zA-Z0-9_.]*" metric) (str "Invalid metric name: " metric))
  (assert (< (count metric) 200) (str "Metric name too long: " metric))
  (str metric
       ":" value
       "|" type
       (when-not (== 1 sample-rate)
         (str "|@" sample-rate))
       (let [global-tags (:tags @*state)]
         (when (or (not-empty tags)
                   (not-empty global-tags))
           (str "|#" (format-tags global-tags tags))))))


(defn- report-fn [type]
  (fn report!
    ([name value] (report! name value {}))
    ([name value opts]
      (let [tags        (:tags opts [])
            sample-rate (:sample-rate opts 1)]
        (when (or (== sample-rate 1)
                  (< (rand) sample-rate))
          (send! (format-metric name type value tags sample-rate)))))))


(def increment! (report-fn "c"))
  

(def gauge! (report-fn "g"))


(def histogram! (report-fn "h"))


(defmacro measure! [metric opts & body]
  `(let [t0#  (System/currentTimeMillis)
         res# (do ~@body)]
     (histogram! ~metric (- (System/currentTimeMillis) t0#) ~opts)
     res#))


(def set! (report-fn "s"))


(defn- escape-event-string [s]
  (str/replace s "\n" "\\n"))


(defn- format-event [title text opts]
  (let [title' (escape-event-string title)
        text'  (escape-event-string text)
        {:keys [tags date-happened hostname aggregation-key
                priority source-type-name alert-type]} opts]
    (str "_e{" (count title') "," (count text') "}:" title' "|" text'
         (when date-happened
           (assert (instance? java.util.Date date-happened))
           (str "|d:" (-> date-happened .getTime (/ 1000) long)))
         (when hostname
           (str "|h:" hostname))
         (when aggregation-key
           (str "|k:" aggregation-key))
         (when priority
           (assert (#{:normal :low} priority))
           (str "|p:" (name priority)))
         (when source-type-name
           (str "|s:" source-type-name))
         (when alert-type
           (assert (#{:error :warning :info :success} alert-type))
           (str "|t:" (name alert-type)))
         (let [global-tags (:tags @*state)]
           (when (or (not-empty tags)
                     (not-empty global-tags))
             (str "|#" (format-tags global-tags tags)))))))


(defn event!
  "title => String
   text  => String
   opts  => { :tags             => [String+] | { Keyword -> Any | Nil }
              :date-happened    => #inst
              :hostname         => String
              :aggregation-key  => String
              :priority         => :normal | :low
              :source-type=name => String
              :alert-type       => :error | :warning | :info | :success }"
  [title text opts]
  (let [payload (format-event title text opts)]
    (assert (< (count payload) (* 8 1024)) (str "Payload too big: " title text payload))
    (send! payload)))
