(ns task-list.core
  (:require
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]))

(defonce server (atom nil))

(defn respond-hello [request]
  (let [nm (get-in request [:query-params :name])]
    {:status 200 :body (str "Hello, " nm "\n")}))

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]}))

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

