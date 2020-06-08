package uk.co.gresearch.spark.dgraph.connector

import io.dgraph.DgraphClient
import io.dgraph.DgraphProto.Response
import io.grpc.ManagedChannel
import org.apache.spark.sql.connector.read.InputPartition

/**
 * Partition of Dgraph data. Reads all triples with the given predicates in the given uid range.
 *
 * @param targets Dgraph alpha nodes
 * @param predicates optional predicates to read
 * @param uids optional uid ranges
 */
case class Partition(targets: Seq[Target], predicates: Option[Set[Predicate]], uids: Option[UidRange]) extends InputPartition {

  // TODO: use host names of Dgraph alphas to co-locate partitions
  override def preferredLocations(): Array[String] = super.preferredLocations()

  /**
   * Reads the entire partition and returns all triples.
   *
   * @return triples
   */
  def getTriples: Iterator[Triple] = {
    val query =
      predicates
        .map(Query.forPropertiesAndEdges("data", _, uids))
        .getOrElse(Query.forAllPropertiesAndEdges("data", uids))

    readTriples(query, None)
  }

  /**
   * Reads the entire partition and returns all edge triples.
   *
   * @return triples
   */
  def getEdgeTriples: Iterator[Triple] = {
    val query =
      predicates
        // returns only edges due to schema
        .map(Query.forPropertiesAndEdges("data", _, uids))
        // returns properties and edges, requires filtering for edges (see below)
        .getOrElse(Query.forAllPropertiesAndEdges("data", uids))

    val nodeTriplesFilter: Option[Triple => Boolean] =
      predicates
        // no filtering
        .map(_ => None)
        // filter for true edges
        .getOrElse(Some((t: Triple) => t.o.isInstanceOf[Uid]))

    readTriples(query, nodeTriplesFilter)
  }

  /**
   * Reads the entire partition and returns all node triples.
   *
   * @return triples
   */
  def getNodeTriples: Iterator[Triple] = {
    val query =
      predicates
        .map(Query.forPropertiesAndEdges("data", _, uids))
        .getOrElse(Query.forAllProperties("data", uids))
    readTriples(query, None)
  }

  /**
   * Sends the query, parses the Json response into triples and filters with the optional filter.
   * @param query dgraph query
   * @param triplesFilter optional filter for triples
   * @return triples
   */
  private def readTriples(query: String, triplesFilter: Option[Triple => Boolean]): Iterator[Triple] = {
    val channels: Seq[ManagedChannel] = targets.map(toChannel)
    try {
      val client: DgraphClient = getClientFromChannel(channels)
      val response: Response = client.newReadOnlyTransaction().query(query)
      val json: String = response.getJson.toStringUtf8
      val triples = TriplesFactory.fromJson(json, "data", predicates)
      triplesFilter.map(triples.filter).getOrElse(triples)
    } finally {
      channels.foreach(_.shutdown())
    }
  }

}