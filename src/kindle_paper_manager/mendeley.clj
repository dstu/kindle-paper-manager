(ns kindle-paper-manager.mendeley
  "Mendeley data import utilities."
  (:require [clojure.contrib.sql :as sql]))

(def *default-data-directory*
     (java.io.File. (str (System/getProperty "user.home") "/local/share/data/Mendeley Ltd./Mendeley Desktop")))

(defn db-spec [file]
  "Builds connection map suitable for passing to with-connection
to open the SQLite database in file."
  {:classname    "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     (.getAbsolutePath file)})

(defmacro with-data-file [file & body]
  "Connects to the Mendeley database in file and executes body."
  `(sql/with-connection (db-spec ~file) ~@body))

(defn database-files
  ([]
     "Seq of .sqlite database files in *default-data-directory*."
     (database-files *default-data-directory*))
  ([data-directory]
     "Seq of .sqlite database files in data-directory."
     (->> (.listFiles data-directory)
	  (filter #(.endsWith (.getName %) ".sqlite")))))

(defn documents []
  "Seq of documents in the open Mendeley database."
  (sql/with-query-results rs ["select d.id, d.title from Documents d"] (doall rs)))

(defn documents-with-files []
  "Seq of documents with local files in the open Mendeley
database."
  (sql/with-query-results rs ["select d.title, df.hash as sha1, f.localUrl as file from Documents d inner join DocumentFiles df on d.id = df.documentId inner join Files f on f.hash = df.hash where f.localUrl like 'file://%';"]
    (doall
     (map #(let [localUrl (:file %)
		 filePath (.substring localUrl (.length "file://"))]
	     (assoc % :file filePath))) rs)))
