package com.cwheikki.readsidebenchmark.impl

import java.util.UUID

import com.cwheikki.readsidebenchmark.impl.ReadSideBenchmarkRepository.ReadSideBenchMarkRecord
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import slick.dbio.{Effect, NoStream}
import slick.sql.FixedSqlAction

class ReadSideBenchmarkProcessor(readSide: SlickReadSide, repository: ReadSideBenchmarkRepository) extends ReadSideProcessor[ReadsidebenchmarkEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[ReadsidebenchmarkEvent] =
    readSide
      .builder[ReadsidebenchmarkEvent]("readside-benchmark")
      .setGlobalPrepare(repository.createTable())
      .setEventHandler[BenchmarkEventPersisted](handleEvent)
      .build()

  private def handleEvent(event: EventStreamElement[BenchmarkEventPersisted]): FixedSqlAction[Int, NoStream, Effect.Write] = {
    val entityId = event.entityId
    val eventCreatedTime = event.event.timestamp
    val testId = event.event.testId
    val totalEventsPersisted = event.event.eventsPersisted
    val refreshInterval = event.event.refreshInterval
    val queryDelay = event.event.queryDelay
    //val uuid = UUID.nameUUIDFromBytes(s"$testId-$entityId-$totalEventsPersisted-$refreshInterval-$queryDelay".getBytes).toString
    val uuid = UUID.randomUUID().toString

    val readSideCreationTime = System.nanoTime()
    val delta = readSideCreationTime - eventCreatedTime
    val entry = ReadSideBenchMarkRecord(uuid, eventCreatedTime, readSideCreationTime, delta, testId, totalEventsPersisted, refreshInterval, queryDelay)

    repository.insert(entry)
  }

  override def aggregateTags: Set[AggregateEventTag[ReadsidebenchmarkEvent]] = ReadsidebenchmarkEvent.Tag.allTags
}
