(ns com.freiheit.gae.datastore.transactions
  (:require
   [clojure.contrib.logging :as log]
   [com.freiheit.clojure.util.exceptions :as exceptions])
  (:import [com.google.appengine.api.datastore
            DatastoreServiceFactory]))

;;;; Transaction support for the appengine.

;; ------------------------------------------------------------------------------
;; public functions
;; ------------------------------------------------------------------------------
(defn begin-transaction
  "Begin a datastore transaction. Returns a new transaction."
  []
  (.beginTransaction (DatastoreServiceFactory/getDatastoreService)))

(defn commit-transaction
  "Commit a transaction."
  [transaction]
  (.commit transaction))

(defn rollback-transaction
  "Rollback a transaction."
  [transaction]
  (.rollback transaction))

(defn current-transaction
  "Return the current transaction."
  []
  (.getCurrentTransaction (DatastoreServiceFactory/getDatastoreService)))

(defmacro with-transaction
  "Encapsulate code with a datastore transaction. The transaction is commited at the end."
  [& body]
  `(let [transaction# (begin-transaction)]
     (let [body# (do ~@body)]
       (commit-transaction transaction#)
       body#)))

(defmacro with-transaction-rethrow
  "Encapsulate code with a datastore transaction. The transaction is commited at the end. If an exception
   has occured then the transaction is rolled back. The exception is rethrown."
  [& body]
  `(let [transaction# (begin-transaction)]
     (try 
	(let [body# (do ~@body)]
          (commit-transaction transaction#)
          body#)
	(catch Exception e#
	    (do 
	      (log/log :error (str "Exception during transaction.\n\n"
                                   (exceptions/get-stack-trace e#)))
	      (when (.isActive transaction#)
		(rollback-transaction transaction#))
	      (throw e#))))))
