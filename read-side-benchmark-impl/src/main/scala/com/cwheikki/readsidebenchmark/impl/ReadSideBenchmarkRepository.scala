package com.cwheikki.readsidebenchmark.impl

import com.cwheikki.readsidebenchmark.impl.ReadSideBenchmarkRepository.ReadSideBenchMarkRecord
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction

class ReadSideBenchmarkRepository(database: Database) {

  class ReadSideBenchmarkTable(tag: Tag) extends Table[ReadSideBenchMarkRecord](tag, "read_side_benchmark") {
    def eventId = column[String]("event_id", O.PrimaryKey)
    def eventCreatedTimestamp = column[Long]("event_created_timestamp")
    def readSideCreatedTimestamp = column[Long]("read_side_created_timestamp")
    def timeToUpdateNs = column[Long]("time_to_update_ns")
    def testId = column[String]("test_id")
    def totalEventsPersisted = column[Int]("total_events_persisted")
    def refreshIntervalMs = column[Int]("refresh_interval_ms")
    def queryDelayMs = column[Int]("query_delay_ms")

    def * =
      (
        eventId,
        eventCreatedTimestamp,
        readSideCreatedTimestamp,
        timeToUpdateNs,
        testId,
        totalEventsPersisted,
        refreshIntervalMs,
        queryDelayMs
      ) <> ((ReadSideBenchMarkRecord.apply _).tupled, ReadSideBenchMarkRecord.unapply)
  }

  val readSideBenchmarkTable = TableQuery[ReadSideBenchmarkTable]

  def createTable(): FixedSqlAction[Unit, NoStream, Effect.Schema] =
    readSideBenchmarkTable.schema.createIfNotExists

  def insert(entry: ReadSideBenchMarkRecord) = {
    readSideBenchmarkTable += entry
  }
}

object ReadSideBenchmarkRepository {
  case class ReadSideBenchMarkRecord(eventId: String,
                                     eventCreatedTimestamp: Long,
                                     readSideCreatedTimestamp: Long,
                                     timeToUpdateNs: Long,
                                     testId: String,
                                     totalEventsPersisted: Int,
                                     refreshIntervalMs: Int,
                                     queryDelayMs: Int)
}
