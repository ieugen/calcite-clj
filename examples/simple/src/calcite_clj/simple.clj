(ns calcite-clj.simple
  (:require [com.rpl.proxy-plus :refer [proxy+]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import (java.util List
                      Map
                      Map$Entry)
           (org.apache.calcite DataContext)
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
(def depts {:rowdef [{:name "DEPTNO" :type SqlTypeName/INTEGER}
                     {:name "NAME" :type SqlTypeName/VARCHAR}]
            :data [[10 "Sales"]
                   [20	"Marketing"]
                   [30 "Accounts"]]})

;; EMPNO:int	NAME:string	DEPTNO:int	GENDER:string	CITY:string	EMPID:int
;; AGE:int	SLACKER:boolean	MANAGER:boolean	JOINEDAT:date
(def emps {:rowdef [{:name "EMPNO" :type SqlTypeName/INTEGER}
                    {:name "NAME" :type SqlTypeName/VARCHAR}
                    {:name "DEPTNO" :type SqlTypeName/INTEGER}
                    {:name "GENDER" :type SqlTypeName/VARCHAR}
                    {:name "CITY" :type SqlTypeName/VARCHAR}
                    {:name "EMPID" :type SqlTypeName/INTEGER}
                    {:name "AGE" :type SqlTypeName/INTEGER}
                    {:name "SLACKER" :type SqlTypeName/BOOLEAN}
                    {:name "MANAGER" :type SqlTypeName/BOOLEAN}
                    {:name "JOINDATE" :type SqlTypeName/DATE}]
           :data [[100 "Fred" 10 nil nil 30 25 true false "1996-08-03"]
                  [110	"Eric"	20	"M"	"San Francisco"	3	80		nil false	"2001-01-01"]
                  [110	"John"	40	"M"	"Vancouver"	2	false	true	"2002-05-03"]
                  [120	"Wilma"	20	"F"	 nil	1	5	nil true "2005-09-07"]
                  [130	"Alice"	40	"F"	"Vancouver"	2	false	true	"2007-01-01"]]})

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
  (.createStructType type-factory ^List (map (partial row->rel-type type-factory) rowdef)))

(defn scannable-table
  "Table based on a clojure data structure.
   It implements the {@link ScannableTable} interface, so Calcite gets
   data by calling the {@link #scan(DataContext)} method."
  ^Table [{:keys [data rowdef] :as table-def}]
  (let []
    (proxy+
     []
     AbstractTable
     (getRowType
      [this ^RelDataTypeFactory type-factory]
      (row-type type-factory rowdef))

     ScannableTable
     (scan
      [this ^DataContext root]
      (let [type-factory (.getTypeFactory root)
            ;; field-types (get-field-types type-factory source field-types-x is-stream)
            ;; fields (ImmutableIntList/identity (.size field-types))
            ;; cancel-flag (.get DataContext$Variable/CANCEL_FLAG root)
            ]
        (proxy+
         []
         AbstractEnumerable
         (enumerator
          [this]
          (throw (UnsupportedOperationException. "Not implemented"))
          )))))))

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


  #_(require '[clojure.tools.trace :as trace])
  #_(trace/trace-ns 'ro.ieugen.calcite-csv)

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
