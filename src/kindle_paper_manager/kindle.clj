(ns kindle-paper-manager.kindle
  "Kindle data import and export utilities."
  (:require [clojure.contrib.io :as io])
  (:require [clojure.contrib.json :as json])
  (:require [digest]))

(def *default-kindle-root* (java.io.File. "/mnt/kindle"))

(def *hash-prefix* "/mnt/us/documents/")

(def *relative-collections-path*
     (str "system" (System/getProperty "file.separator") "collections.json"))

(def *default-collection-locale* "en-US")

(defn collections-file
  ([]
     "Creates a file corresponding to the collections file in a
Kindle mounted at *default-kindle-root*."
     (collections-file *default-kindle-root*))
  ([^java.io.File kindle-root]
     {:pre [(.exists kindle-root)
	    (.canRead kindle-root)]}
     "Creates a file corresponding to the collections file in a
Kindle mounted at kindle-root."
     (java.io.File. kindle-root *relative-collections-path*)))

(defn read-collections
  ([^java.io.File collections-file]
     "Reads the content of collections in collections-file in the
locale *default-collection-locale* into a seq."
     (read-collections collections-file *default-collection-locale*))
  ([^java.io.File collections-file desired-locale]
     {:pre [(.exists collections-file)
	    (.canRead collections-file)]}
     "Reads the content of collections in collections-file in
the locale desired-locale into a map of collection name to file
sha1s."
     (-> (with-open [in (java.io.FileReader. collections-file)]
	   (json/read-json in))
	 (filter (fn [[k v]] (.endsWith (str k) (str "@" desired-locale))))
	 (map (fn [[k v]] {k (:items v)})))))

(defn hash-filename [filename]
  (digest/sha-1 (str *hash-prefix* filename)))

(defn read-files
  ([]
     "Reads the files in the Kindle documents directory for a
Kindle rooted at *default-kindle-root* and maps them to mangled
SHA-1s of their mangled names."
     (read-files (java.io.File. *default-kindle-root* "documents")))
  ([^java.io.File documents-dir]
     "Reads the files in documents-dir and maps them to mangled
SHA-1s of their mangled names."
     {:pre [(.exists documents-dir)
	    (.canRead documents-dir)
	    (.isDirectory documents-dir)]}
     (let [document-files (.list documents-dir)
	   document-hashes (map #(str "*" (hash-filename %)) document-files)]
       (zipmap document-hashes document-files))))

;; Convert collections to (incomplete) records

;; Export records to Kindle (generate a collections.json and
;; other Kindle metadata from a coll of records, copy needed
;; files over)

(defn collections [records])
