/*
 * Copyright 2020 G-Research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.gresearch.spark.dgraph.connector

import org.apache.spark.sql.execution.ProjectExec
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2ScanExec
import org.apache.spark.sql.{Column, Dataset, Row}
import org.scalatest.Assertions
import uk.co.gresearch.spark.dgraph.connector.Predicate.columnNameForPredicateName
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
                                    expectedDs: Set[T]): Unit = {
    val expectedOutput =
      expectedProjection.map(_.map(p => columnNameForPredicateName(p.predicateName)).toSet)
        .getOrElse(ds.columns.toSet)
    val projectedDs = if (selection.nonEmpty) ds.select(selection: _*) else ds
    val plan = projectedDs.queryExecution.sparkPlan
    val root = plan match {
      case ProjectExec(_, child) => child
      case _ => plan
    }
    assert(root.isInstanceOf[DataSourceV2ScanExec])

    val scan = root.asInstanceOf[DataSourceV2ScanExec]
    assert(scan.outputSet.map(_.name).toSet === expectedOutput)

    val actual = projectedDs.collect()
    assert(actual.toSet === expectedDs)
    assert(actual.length === expectedDs.size)
  }

  def select(idx: Int*)(row: Row): Row = Row(idx.map(row.get): _*)

}
