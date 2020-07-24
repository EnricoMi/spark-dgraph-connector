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

import org.apache.spark.sql.sources.v2.DataSourceOptions
import uk.co.gresearch.spark.dgraph.connector._

class ConfigPartitionerOption extends PartitionerProviderOption
  with EstimatorProviderOption
  with ClusterStateHelper
  with ConfigParser {

  override def getPartitioner(schema: Schema,
                              clusterState: ClusterState,
                              options: DataSourceOptions): Option[Partitioner] =
    getStringOption(PartitionerOption, options)
      .map(getPartitioner(_, schema, clusterState, options))

  def getPartitioner(partitionerName: String,
                     schema: Schema,
                     clusterState: ClusterState,
                     options: DataSourceOptions): Partitioner =
    partitionerName match {
      case SingletonPartitionerOption => SingletonPartitioner(getAllClusterTargets(clusterState), schema)
      case GroupPartitionerOption => GroupPartitioner(schema, clusterState)
      case AlphaPartitionerOption =>
        AlphaPartitioner(schema, clusterState,
          getIntOption(AlphaPartitionerPartitionsOption, options, AlphaPartitionerPartitionsDefault))
      case PredicatePartitionerOption =>
        PredicatePartitioner(schema, clusterState,
          getIntOption(PredicatePartitionerPredicatesOption, options, PredicatePartitionerPredicatesDefault))
      case UidRangePartitionerOption =>
        val uidsPerPartition = getIntOption(UidRangePartitionerUidsPerPartOption, options, UidRangePartitionerUidsPerPartDefault)
        val estimator = getEstimatorOption(UidRangePartitionerEstimatorOption, options, UidRangePartitionerEstimatorDefault, clusterState)
        val targets = getAllClusterTargets(clusterState)
        val singleton = SingletonPartitioner(targets, schema)
        UidRangePartitioner(singleton, uidsPerPartition, estimator)
      case option if option.endsWith(s"+${UidRangePartitionerOption}") =>
        val name = option.substring(0, option.indexOf('+'))
        val partitioner = getPartitioner(name, schema, clusterState, options)
        getPartitioner(UidRangePartitionerOption, schema, clusterState, options)
          .asInstanceOf[UidRangePartitioner].copy(partitioner = partitioner)
      case unknown => throw new IllegalArgumentException(s"Unknown partitioner: $unknown")
    }

}
