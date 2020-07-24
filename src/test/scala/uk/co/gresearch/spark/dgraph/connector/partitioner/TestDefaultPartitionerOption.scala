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

package uk.co.gresearch.spark.dgraph.connector.partitioner

import java.util.UUID

import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.scalatest.FunSpec
import uk.co.gresearch.spark.dgraph.connector.{ClusterState, Predicate, Schema, Target}

class TestDefaultPartitionerOption extends FunSpec {

  describe("DefaultPartitionerOption") {
    val target = Target("localhost:9080")
    val schema = Schema(Set(Predicate("pred", "string")))
    val state = ClusterState(
      Map("1" -> Set(target)),
      Map("1" -> schema.predicates.map(_.predicateName)),
      10000,
      UUID.randomUUID()
    )
    val options = DataSourceOptions.empty()

    it(s"should provide a partitioner") {
      new DefaultPartitionerOption().getPartitioner(schema, state, options)
    }
  }
}
