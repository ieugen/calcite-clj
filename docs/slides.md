# First steps using Apache Calcite & Clojure

Eugen Stan <eugen.stan@netdava.com>

`@ieugen` on internet (mostly)

Apache Calcite Online Meetup

2022-01-19



## Abstract

A quick introduction to `calcite-clj` .

A library for using Apache Calcite with  Clojure language.

Links:
* https://github.com/ieugen/calcite-clj
* https://clojars.org/io.github.ieugen/calcite-clj



## Ză (the) plan

* 1 min intor to Apache Calcite and Clojure
* My motivation for doing this work
* How the integration works
* Future directions
* Code examples and demos



## Apache Calcite

The foundation for your next high-performance database.

Use Calcite + add persistence to build your database.


Apache Calcite is a Java library for:
* SQL parsing
* Relational algebra
* JDBC driver

(Also see Calcite adapters)



## Clojure

* Concise functional languge
* Fast to prototype via REPL
* Immutable (persistent) data structures by default
* Low code churn in ecosystem



## Why Calcite + Clojure

* They both start with C and and with e
* They have same number of letters !


## Why Calcite + Clojure (2)

* I think they are a good fit together
* Would &#9829; to use Calcite from Clojure
* Explore if I can expose SQL query language to web developers


## What calcite-clj offers

Use a clojure function as a Calcite schema factory.

Implement Calcite adapters in Clojure.



## How to use calcite-clj?

Follow these easy steps ...


### Add `calcite-clj` to your project

deps.edn
```clj
io.github.ieugen/calcite-clj {:mvn/version "0.1.2"}
```
Gradle
```
implementation("io.github.ieugen:calcite-clj:0.1.2")
```

(See https://clojars.org/io.github.ieugen/calcite-clj)


### The schema factory defn

Define a function that builds your schema

```clj
(ns calcite-clj.simple
  (:require [com.rpl.proxy-plus :refer [proxy+]]])
  (:import (org.apache.calcite.schema Schema)
           (org.apache.calcite.schema.impl AbstractSchema)))

(defn create-tables [] ... )

(defn my-schema
  [parent-schema name operand]
  (let [tables (create-tables)]
    (proxy+
     []
     AbstractSchema
     (getTableMap [this] tables))))
```


### Write the model

Build you `model.json` file.

```json
 {
    "version": "1.0",
    "defaultSchema": "SALES",
    "schemas": [{
        "name": "SALES",
        "type": "custom",
        "factory": "ro.ieugen.calcite.clj.SchemaFactory",
        "operand": {
          "clojure-clj.schema-factory":
          "calcite-clj.simple/my-schema"
        } } ] }
```

Notice `ro.ieugen.calcite.clj.SchemaFactory` and
`"clojure-clj.schema-factory": "calcite-clj.simple/my-schema"`


## Use via JDBC

Reference the model and let Calcite and Clojure do the magic.

```clj
  (let [db {:jdbcUrl "jdbc:calcite:model=resources/model.json"
            :user "admin"
            :password "admin"}
        ds (jdbc/get-datasource db)
        query "select * from emps where age
               is null or age >= 40"]
    (jdbc/execute! ds [query]))
```



## How the integration works?

Main work is done by `ro.ieugen.calcite.clj.SchemaFactory`

```java
  public Schema create(SchemaPlus parentSchema,
     String name, Map<String, Object> operand) {

    final String sfqn =
     (String) operand.get("clojure-clj.schema-factory");

    IFn ifn = Clojure.var(sfqn);
    Schema s = (Schema) ifn.invoke(parentSchema,
    name, operand);
    return s;
  }
```



## Future directions

* Improve `calcite-clj` so users can write idiomatic Clojure
* See how far OFBiz entity engine can go on top of Calcite
* Experiment with adding Datmoic Datalog on top of Calcite


## Code

Demo time!



## Contact and project links

* `@ieugen` on internet (mostly)
* Email: <eugen.stan@netdava.com>
* Website: https://ieugen.ro/
* Company website: https://netdava.com/
* https://github.com/ieugen/calcite-clj
* https://clojars.org/io.github.ieugen/calcite-clj
* https://github.com/ieugen/ofbiz-tooling-ro
