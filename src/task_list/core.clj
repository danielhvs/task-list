(ns task-list.core
  (:require
    [clojure.edn :as edn]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [io.pedestal.test :as test]))

(defonce server (atom nil))
(defonce db (atom {}))

(defn ok
  ([]
   {:status 200
    :headers {"Content-Type" "text/html"}})
  ([body]
   (-> (ok)
       (assoc :body body))))

(defn- new-task
  [task-name]
  {:name task-name
   :status :todo})

(defn str->uuid [id]
  (java.util.UUID/fromString id))

(defn get-from-store [id]
  (let [uuid (str->uuid id)]
    (get @db uuid)))

(defn get-task [request]
  (let [id (get-in request [:query-params :id])]
    (ok (get-from-store id))))

(defn create-task [request]
  (let [task-name (get-in request [:query-params :name])
        uuid (java.util.UUID/randomUUID)
        task (new-task task-name)]
    (swap! db assoc uuid task)
    (ok {:task task
         :id uuid})))

(defn delete-task [request]
  (let [id (get-in request [:query-params :id])
        uuid (str->uuid id)]
    (swap! db dissoc uuid)
    (ok)))

(def routes
  (route/expand-routes
    #{["/task" :get get-task :route-name :get-task]
      ["/task" :post create-task :route-name :create-task]
      ["/task" :delete delete-task :route-name :delete-task]}))

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

; test
(restart)

(defn test-api [verb url]
  (test/response-for (::http/service-fn @server) verb url))

(defn json->clj [json]
  (edn/read-string json))

(comment
  (let [id (->> "/task?name=oi"
                (test-api :post)
                :body
                json->clj
                :id)]
    (test-api :get (str "/task?id=" id))))

(test-api :delete (str "/task?id=687d149c-ec5c-4ad0-8535-983da4f2decf"))
