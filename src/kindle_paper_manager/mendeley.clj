(ns kindle-paper-manager.mendeley
  "Mendeley data import utilities."
  (:require [clojure.contrib.sql :as sql]))

(def *default-data-directory*
     (java.io.File. (str (System/getProperty "user.home") "/.local/share/data/Mendeley Ltd./Mendeley Desktop")))

(defn db-spec [file]
  "Builds connection map suitable for passing to with-connection
to open the SQLite database in file."
  {:classname   "org.sqlite.JDBC"
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
  (sql/with-query-results rs ["select d.id, d.title, df.hash as sha1, f.localUrl as file from Documents d inner join DocumentFiles df on d.id = df.documentId inner join Files f on f.hash = df.hash where f.localUrl like 'file://%';"]
    (doall
     (map #(let [localUrl (:file %)
		 filePath (.substring localUrl (.length "file://"))]
	     (assoc % :file filePath)) rs))))

(defn document-folders []
  "Map from document ID to folders in the open Mendeley
database."
  (sql/with-query-results rs ["select d.id, f.name as folder from documents d inner join documentfolders df on d.id=df.documentId inner join folders f on f.id=df.folderId;"]
    (reduce #(let [id (:id %2)
		   new-folder (:folder %2)
		   folders (get %1 id)]
	       (assoc %1 id (cons new-folder folders))) {} (doall rs))))

(defn document-authors []
  "Map from document ID to authors in the open Mendeley
database."
  (sql/with-query-results rs ["select d.id, c.firstNames, c.lastName from documents d inner join documentcontributors c on d.id=c.documentid;"]
    (reduce #(let [id (:id %2)
		   new-author [(:firstnames %2) (:lastname %2)]
		   authors (get %1 id)]
    	       (assoc %1 id (cons new-author authors))) {} (doall rs))))

(defn document-records []
  "Seq of document records found in the open Mendeley
database. Elements have keys :authors, :groups, :sha1, :file,
and :title."
  (let [authors (document-authors)
	groups (document-folders)]
    (map (fn [record]
	   (-> record
	       (assoc :authors (get authors (:id record)))
	       (assoc :groups (get groups (:id record))))) (documents-with-files))))

(defn load-records [file]
  (with-data-file file (document-records)))
