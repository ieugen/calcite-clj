;;
;; Build instructions
;;
(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))


(def lib 'io.github.ieugen/calcite-clj)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def src-dirs ["src/main/java"])

(defn javac [opts] (b/javac opts) opts)

(defn jar "Assemble jar from classes and pom."
  [opts]
  (b/jar opts)
  opts)

(defn write-pom [opts] (b/write-pom opts) opts)

(defn clean [opts]
  (b/delete {:path "target"})
  opts)

(defn ci "Run the CI pipeline of tests (and build the JAR)."
  [params]
  (let [params (assoc params :lib lib
                      :version version
                      :basis basis
                      :javac-opts ["-source" "8" "-target" "8"
                                   ;; disable annotation processing
                                   "-proc:none"]
                      :src-dirs src-dirs
                      :class-dir class-dir
                      :jar-file jar-file
                      :scm {:connection "https://github.com/ieugen/calcite-clj.git"
                            :developerConnection "https://github.com/ieugen/calcite-clj.git"
                            :url "https://github.com/ieugen/calcite-clj"})]
    (clean params)
    (javac params)
    (write-pom params)
    (jar params)
    params))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (b/install)))

(defn deploy "Deploy the JAR to Clojars."
  [params]
  (let [params (merge {:installer :remote
                       :lib lib
                       :version version
                       :artifact jar-file
                       :class-dir class-dir}
                      params)
        pom-file (b/pom-path params)
        params (assoc params :pom-file pom-file)]
    (dd/deploy params)))

(comment

  (clean nil)
  (ci nil)
  (deploy nil)


  )