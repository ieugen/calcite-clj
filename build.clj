;;
;; Build instructions
;;
(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))


(def lib 'io.github.ieugen/calcite-clj)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def src-dirs ["src/main/java"])

(defn javac [opts] (b/javac opts) opts)

(defn jar "Assemble jar from classes and pom." [opts] (b/jar opts) opts)

(defn write-pom [opts] (b/write-pom opts) opts)

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib
             :version version
             :basis basis
             :javac-opts ["-source" "8" "-target" "8"]
             :src-dirs src-dirs
             :class-dir class-dir
             :jar-file jar-file)
      (bb/clean)
      (javac)
      (write-pom)
      (jar)))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))