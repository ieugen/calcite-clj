package ro.ieugen.calcite.clj;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import java.util.Map;
import java.util.Objects;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

/**
 * Schema factory that delegates processing to Clojure namespace/fn .
 * <p>
 * Expects an operand with the following key-pair: "clojure-clj.schema-factory":
 * "my.example.ns/my-schema-factory-fn"
 * <p>
 * Will delegate all arguments to that clojure function. Clojure function must return a {@link
 * Schema}
 */
@SuppressWarnings("UnusedDeclaration")
public class SchemaFactory implements org.apache.calcite.schema.SchemaFactory {

  public static final SchemaFactory INSTANCE = new SchemaFactory();

  public SchemaFactory() {
  }

  @Override
  public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
    final String schemaFactoryQualifiedName = (String) operand.get("clojure-clj.schema-factory");
    Objects.requireNonNull(schemaFactoryQualifiedName, "clojure-clj.schema-factory");
    if (schemaFactoryQualifiedName.isBlank()) {
      throw new IllegalArgumentException("clojure-clj.schema-factory is blank");
    }
    IFn schemaFactoryFn = Clojure.var(schemaFactoryQualifiedName);
    Schema s = (Schema) schemaFactoryFn.invoke(parentSchema, name, operand);
    return s;
  }
}
