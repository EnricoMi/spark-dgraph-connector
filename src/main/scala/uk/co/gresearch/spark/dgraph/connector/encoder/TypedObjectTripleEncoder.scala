package uk.co.gresearch.spark.dgraph.connector.encoder

import java.sql.Timestamp

import org.apache.spark.sql.Encoders
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String
import uk.co.gresearch.spark.dgraph.connector.{DGraphTypedObjectRow, Geo, Password, Triple, TriplesFactory, Uid}

/**
 * Encodes Triple by representing objects in multiple typed columns.
 **/
class TypedObjectTripleEncoder extends TripleEncoder {

  /**
   * Returns the schema of this table. If the table is not readable and doesn't have a schema, an
   * empty schema can be returned here.
   * From: org.apache.spark.sql.connector.catalog.Table.schema
   */
  override def schema(): StructType = Encoders.product[DGraphTypedObjectRow].schema

  /**
   * Returns the actual schema of this data source scan, which may be different from the physical
   * schema of the underlying storage, as column pruning or other optimizations may happen.
   * From: org.apache.spark.sql.connector.read.Scan.readSchema
   */
  override def readSchema(): StructType = schema()

  /**
   * Encodes a triple as an InternalRow.
   *
   * @param triple a Triple
   * @return an InternalRow
   */
  override def asInternalRow(triple: Triple): InternalRow = {
    val objectType = TriplesFactory.getType(triple.o)

    // order has to align with DGraphTypedObjectRow
    val valuesWithoutObject = Seq(
      triple.s.uid,
      UTF8String.fromString(triple.p),
      null, // uid
      null, // string
      null, // long
      null, // double
      null, // timestamp
      null, // boolean
      null, // geo
      null, // password
      UTF8String.fromString(objectType)
    )

    // order has to align with DGraphTypedObjectRow
    val (objectValueIndex, objectValue) =
      objectType match {
        case "uid" => (2, triple.o.asInstanceOf[Uid].uid)
        case "string" => (3, UTF8String.fromString(triple.o.asInstanceOf[String]))
        case "long" => (4, triple.o)
        case "double" => (5, triple.o)
        case "timestamp" => (6, DateTimeUtils.fromJavaTimestamp(triple.o.asInstanceOf[Timestamp]))
        case "boolean" => (7, triple.o)
        case "geo" => (8, UTF8String.fromString(triple.o.asInstanceOf[Geo].geo))
        case "password" => (9, UTF8String.fromString(triple.o.asInstanceOf[Password].password))
        case "default" => (3, UTF8String.fromString(triple.o.toString))
        case _ => (3, UTF8String.fromString(triple.o.toString))
      }
    val values = valuesWithoutObject.updated(objectValueIndex, objectValue)

    InternalRow(values: _*)
  }

}