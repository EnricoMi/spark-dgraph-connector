package uk.co.gresearch.spark.dgraph.connector

import org.apache.spark.sql.execution.ProjectExec
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2ScanExec
import org.apache.spark.sql.{Column, Dataset, Row}
import org.scalatest.Assertions
import uk.co.gresearch.spark.dgraph.connector.partitioner.PredicatePartitioner

trait ProjectionPushDownTestHelper extends Assertions {

  /**
   * Tests projection push down. An empty selection is interpreted as no selection.
   *
   * @param ds dataset
   * @param selection selection
   * @param expectedProjection expected projection
   * @param expectedDs expected dataset
   * @tparam T type of dataset
   */
  def doTestProjectionPushDownDf[T](ds: Dataset[T],
                                    selection: Seq[Column],
                                    expectedProjection: Option[Seq[Predicate]],
                                    expectedUnpushedProjection: Seq[String],
                                    expectedDs: Set[T]): Unit = {
    val projectedDs = if (selection.nonEmpty) ds.select(selection: _*) else ds
    val plan = projectedDs.queryExecution.sparkPlan
    val root = plan match {
      case ProjectExec(project, child) =>
        assert(project.map(_.name) === expectedUnpushedProjection)
        child
      case _ =>
        assert(expectedUnpushedProjection.isEmpty, "some unpushed projections expected but none actually unpushed")
        plan
    }
    assert(root.isInstanceOf[DataSourceV2ScanExec])

    val scan = root.asInstanceOf[DataSourceV2ScanExec]

    val actual = projectedDs.collect()
    assert(actual.toSet === expectedDs)
    assert(actual.length === expectedDs.size)
  }

  def select(idx: Int*)(row: Row): Row = Row(idx.map(row.get): _*)

}
