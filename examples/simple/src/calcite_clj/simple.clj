(ns calcite-clj.simple
  {:clj-kondo/config '{:lint-as {com.rpl.proxy-plus/proxy+ clojure.core/proxy}
                       :linters {:unused-binding {:exclude [this]}}}}
  (:require [com.rpl.proxy-plus :refer [proxy+]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import (java.util List
                      Map
                      Map$Entry)
           (java.util.concurrent.atomic AtomicBoolean)
           (org.apache.calcite DataContext
                               DataContext$Variable)
           (org.apache.calcite.adapter.java JavaTypeFactory)
           (org.apache.calcite.jdbc JavaTypeFactoryImpl)
           (org.apache.calcite.linq4j AbstractEnumerable)
           (org.apache.calcite.rel.type RelDataType
                                        RelDataTypeFactory)
           (org.apache.calcite.schema Schema
                                      SchemaPlus
                                      ScannableTable
                                      Table)
           (org.apache.calcite.schema.impl AbstractSchema
                                           AbstractTable)
           (org.apache.calcite.sql.type SqlTypeName)))

(set! *warn-on-reflection* true)

;; DEPTNO:int	NAME:string
(def depts {:name "DEPTS"
            :rowdef [{:name "DEPTNO" :type SqlTypeName/INTEGER}
                     {:name "NAME" :type SqlTypeName/VARCHAR}]
            :data [[(int 10) "Sales"]
                   [(int 20)	"Marketing"]
                   [(int 30) "Accounts"]]})

;; EMPNO:int	NAME:string	DEPTNO:int	GENDER:string	CITY:string	EMPID:int
;; AGE:int	SLACKER:boolean	MANAGER:boolean	JOINEDAT:date
(def emps {:name "EMPS"
           :rowdef [{:name "EMPNO" :type SqlTypeName/INTEGER}
                    {:name "NAME" :type SqlTypeName/VARCHAR}
                    {:name "DEPTNO" :type SqlTypeName/INTEGER}
                    {:name "GENDER" :type SqlTypeName/VARCHAR}
                    {:name "CITY" :type SqlTypeName/VARCHAR}
                    {:name "EMPID" :type SqlTypeName/INTEGER}
                    {:name "AGE" :type SqlTypeName/INTEGER}
                    {:name "SLACKER" :type SqlTypeName/BOOLEAN}
                    {:name "MANAGER" :type SqlTypeName/BOOLEAN}
                    {:name "JOINDATE" :type SqlTypeName/VARCHAR}]
           :data [[(int 100) "Fred" (int 10) nil nil             (int 30) (int 25) true false "1996-08-03"]
                  [(int 110) "Eric"	(int 51) "M" "San Francisco" (int 3)	(int 80)	nil false	"2001-01-01"]
                  [(int 110) "John"	(int 40) "M" "Vancouver"	   (int 2)	nil false	true	"2002-05-03"]
                  [(int 120) "Wilma" (int 20)	"F"	nil	           (int 1)	(int 5)	nil true "2005-09-07"]
                  [(int 130) "Alice" (int 40)	"F"	"Vancouver"	   (int 2)	nil false	true	"2007-01-01"]]})

(defn to-nullable-rel-data-type
  "Helper to create rel-data-type"
  ^RelDataType [^JavaTypeFactory type-factory ^SqlTypeName sql-type-name]
  (let [sql-type (.createSqlType type-factory sql-type-name)]
    (.createTypeWithNullability type-factory sql-type true)))

(defn row->rel-type
  ^Map$Entry [^JavaTypeFactory type-factory {:keys [name type] :as row}]
  (clojure.lang.MapEntry/create name (to-nullable-rel-data-type type-factory type)))

(defn row-type
  "Build a calcite row type (reltype)from a clojure structure"
  ^RelDataType [^JavaTypeFactory type-factory rowdef]
  ;; (println "row-type" rowdef)
  (.createStructType type-factory ^List (map (partial row->rel-type type-factory) rowdef)))

(defn scannable-table
  "Table based on a clojure data structure.
   It implements the {@link ScannableTable} interface, so Calcite gets
   data by calling the {@link #scan(DataContext)} method."
  ^Table [{:keys [data rowdef name] :as table-def}]
  (let [row-type-memo (memoize row-type)]
    (proxy+
     []
     AbstractTable
     (getRowType
      [this ^RelDataTypeFactory type-factory]
      (row-type-memo type-factory rowdef))

     ScannableTable
     (scan
      [this ^DataContext root]
      (let [type-factory (.getTypeFactory root)
            ^AtomicBoolean cancel-flag (.get DataContext$Variable/CANCEL_FLAG root)]
        (proxy+
         []
         AbstractEnumerable
         (enumerator
          [this]
          (let [i (atom -1)
                len (- (count data) 1)]
            (reify org.apache.calcite.linq4j.Enumerator
              (current [this] (into-array Object (get data @i)))
              (moveNext
                [this]
                (let [res (< @i len)]
                  (if (.get cancel-flag)
                    false
                    (do
                      (when res (swap! i inc))
                      res))))
              (reset [this] (reset! i -1))
              (close [this] nil))))))))))

(defn create-tables
  ^Map []
  {"EMPS" (scannable-table emps)
   "DEPTS" (scannable-table depts)})

(defn my-schema
  "Create a Calcite Schema and return it for use."
  ^Schema [^SchemaPlus parent-schema ^String name ^Map operand]
  (let [tables (create-tables)]
    (proxy+
     []
     AbstractSchema
     (getTableMap [this] tables))))

(comment


  (let [jtf (JavaTypeFactoryImpl.)]
    (to-nullable-rel-data-type jtf SqlTypeName/VARCHAR)
    (row-type jtf (:rowdef depts))
    (row-type jtf (:rowdef emps)))


  (require '[clojure.tools.trace :as trace])
  (trace/trace-ns 'calcite-clj.simple)

  ;; This is the actual main entry point in the application
  ;; We load calcite JDBC driver and instruct it to load our model.json file
  ;; The model.json instructs Calcite to load `ro.ieugen.calcite.clj.SchemaFactory .
  ;; The SchemaFactory implementation will use the operand value for "clojure-clj.schema-factory"
  ;; to know which clojure function to delegate to (i.e. "calcite-clj.simple/my-schema")
  
  (let [db {:jdbcUrl "jdbc:calcite:model=resources/model.json"
            :user "admin"
            :password "admin"}
        ds (jdbc/get-datasource db)]
    (jdbc/execute! ds ["select * from emps where age is null or age >= 40"]))

  ;; display table metadata
  (let [db {:jdbcUrl "jdbc:calcite:model=resources/model.json"
            :user "admin"
            :password "admin"}
        ds (jdbc/get-datasource db)]
    (with-open [connection (jdbc/get-connection ds)]
      (let [metadata (.getMetaData connection)
            rs (.getTables metadata nil nil nil (into-array ["TABLE" "VIEW"]))]
        (rs/datafiable-result-set rs ds))))
  )
