(ns task-list.core
  (:require
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [io.pedestal.test :as test]))

(defonce server (atom nil))

(defn ok [body]
  {:status 200 :body body
   :headers {"Content-Type" "text/html"}})

(def echo
  {:name ::echo
   :enter (fn [context]
            (let [request (:request context)
                  response (ok request)]
              (assoc context :response response)))})

(defn respond-hello [request]
  (let [nm (get-in request [:query-params :name])]
    (ok (str "Hello, " nm "\n"))))

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]
      ["/echo" :get echo]}))

(def service-map
  {::http/routes routes
   ::http/type :jetty
   ::http/port 8890})

(defn stop-dev []
  (when @server
    (http/stop @server)))

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                        (assoc service-map
                          ::http/join? false)))))

(defn restart []
  (stop-dev)
  (start-dev))

(comment
  (restart))

(defn test-api [verb url]
  (test/response-for (::http/service-fn @server) verb url))

(test-api :get "/greet?name=oi")
