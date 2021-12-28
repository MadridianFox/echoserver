(ns echoserver.core
  (:require
    [ring.adapter.jetty :as jetty]
    [ring.middleware.params :as m-params]
    [reitit.ring :as ring-router]
    [clojure.pprint]
    [selmer.parser :as tpl]
    [clojure.string :as string])
  (:gen-class))

(defonce ^:dynamic *server* nil)

(defn dump-handler [req]
  (let [server (select-keys req [:server-port :server-name])
        http (select-keys req [:uri :protocol :request-method :query-string :scheme])
        client (select-keys req [:remote-addr])
        headers (:headers req)
        ext (or (get-in req [:params "format"])
              "html")
        result (tpl/render-file (str "templates/dump." ext) {:server server
                                                      :client client
                                                      :http http
                                                      :headers headers})]
    {:status 200
     :body result}))

(def app
  (ring-router/ring-handler
    (ring-router/router
       [["/" {:get dump-handler}]]
      {:data {:middleware [m-params/wrap-params]}})
    (fn [_] {:status 404 :body "Not Found"})))

(defn start! [{port :port host :host}]
  (alter-var-root #'*server*
    (fn [server]
      (if (nil? server)
        (jetty/run-jetty #'app {:port (Integer. port) :host host :join? false})
        nil))))

(defn stop! []
  (alter-var-root #'*server*
    (fn [server]
      (when (not (nil? server))
        (.stop server))
      nil)))

(defn printHelpAndExit []
  (print "Usage: echoserver --port 8080 --host 127.0.0.1")
  (System/exit 1))

(defn parse-opts [args]
  (reduce (fn [opts item]
            (cond
              (contains? opts :next)
                (-> opts
                  (assoc (:next opts) item)
                  (dissoc :next))
              (#{"--port" "-P"} item)
                (assoc opts :next :port)
              (#{"--host" "-H"} item)
                (assoc opts :next :host)
              (#{"--help" "-h"} item)
                (printHelpAndExit)
              :else
                (printHelpAndExit))) {} args))

(defn with-defaults [opts defaults]
  (reduce (fn [result [key value]]
            (cond
              (not (contains? result key))
              (assoc result key value)
              :else result)) opts defaults))

(defn split-opts [args]
  (reduce (fn [opts item]
            (cond
              (string/includes? item "=")
              (let [[flag value] (string/split item #"=")]
                (conj opts flag value))
              (re-find #"^-[^-].+" item)
              (let [[_ flag value] (re-find #"^(-[^-])(.*)" item)]
                (conj opts flag value))
              :else
                (conj opts item))) [] args))

(defn -main [& args]
  (let [normalized-opts (split-opts args)
        parsed-opts (parse-opts normalized-opts)
        opts (with-defaults parsed-opts {:port 8080 :host "127.0.0.1"})]
    (start! opts)))