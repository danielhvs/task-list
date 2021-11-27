(ns task-list.core
  (:require
    [clojure.edn :as edn]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [io.pedestal.test :as test]
    [ring.util.response :as response]))

(defonce server (atom nil))
(defonce db (atom {}))

(defn ok
  ([]
   {:status 200
    :headers {"Content-Type" "text/html"}})
  ([body]
   (-> (ok)
       (assoc :body body))))

(defn- cycle-status [status] ":todo or :done"
  (status
    {:todo :done
     :done :todo}))

(defn- new-task
  [task-name]
  {:name task-name
   :status :todo})

(defn str->uuid [id]
  (java.util.UUID/fromString id))

(defn get-from-store [id]
  (let [uuid (str->uuid id)]
    (get @db uuid)))

(defn not-found []
  (response/not-found "Not Found"))

(defn get-task [request]
  (let [id (get-in request [:query-params :id])]
    (if-let [task (get-from-store id)]
      (ok task)
      (not-found))))

(defn create-task [request]
  (let [task-name (get-in request [:query-params :name])
        uuid (java.util.UUID/randomUUID)
        task (new-task task-name)]
    (swap! db assoc uuid task)
    (ok {:task task
         :id uuid})))

(defn update-task [request]
  (let [id (get-in request [:path-params :id])]
    (if-let [task (get-from-store id)]
      (do
        (let [updated-task (update-in task [:status] cycle-status)]
          (swap! db assoc (str->uuid id) updated-task)
          (ok updated-task)))
      (not-found))))

(defn delete-task [request]
  (let [id (get-in request [:path-params :id])
        uuid (str->uuid id)]
    (if-let [task (get-from-store id)]
      (do (swap! db dissoc uuid)
          (ok task))
      (response/not-found "Not Found"))))

(def routes
  (route/expand-routes
    #{["/task" :get get-task :route-name :get-task]
      ["/task" :post create-task :route-name :create-task]
      ["/task/:id" :patch update-task :route-name :update-task]
      ["/task/:id" :delete delete-task :route-name :delete-task]}))

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

(let [id (->> "/task?name=tchau"
              (test-api :post)
              :body
              json->clj
              :id)]
  (test-api :get (str "/task?id=" id))
  (test-api :patch (str "/task/" id))
  (test-api :delete (str "/task/" id)))

