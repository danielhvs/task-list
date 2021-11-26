(ns task-list.core
  (:require
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]))

(defonce server (atom nil))

(defn respond-hello [request]
  {:status 200 :body request})

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]}))

(def service-map
  {::http/routes routes
   ::http/type :jetty
   ::http/port 8890})

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                        (assoc service-map
                          ::http/join? false)))))

(defn stop-dev []
  (when @server
    (http/stop @server)))

(defn restart []
  (stop-dev)
  (start-dev))

(comment
  (restart))
