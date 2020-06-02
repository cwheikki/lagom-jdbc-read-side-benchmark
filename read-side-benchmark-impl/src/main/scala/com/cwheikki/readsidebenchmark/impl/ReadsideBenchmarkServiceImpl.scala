package com.cwheikki.readsidebenchmark.impl

import java.util.UUID

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.cwheikki.readsidebenchmark.api.ReadsidebenchmarkService
import com.cwheikki.readsidebenchmark.impl.config.ReadSideConfigValues
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Implementation of the ReadsideBenchmarkService.
  */
class ReadsideBenchmarkServiceImpl(clusterSharding: ClusterSharding, persistentEntityRegistry: PersistentEntityRegistry, config: Config)(
    implicit ec: ExecutionContext,
    mat: Materializer)
    extends ReadsidebenchmarkService {

  private val log = LoggerFactory.getLogger(this.getClass.getSimpleName)

  private val readSideConfigValues = ReadSideConfigValues(config)

  /**
    * Looks up the entity for the given ID.
    */
  private def entityRef(id: String): EntityRef[ReadsidebenchmarkCommand] =
    clusterSharding.entityRefFor(ReadsidebenchmarkState.typeKey, id)

  implicit val timeout = Timeout(5.seconds)

  // TODO if needed, create entities first.
  private def initializeTestEntities(count: Int) = {
    Source((0 to count).toList)
      .map(_.toString)
  }

  private def entityId(keys: String*): String = {
    UUID.nameUUIDFromBytes(keys.mkString(",").getBytes()).toString
  }

  override def runFullBenchmarkTest(testName: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    // Run test...
    runFullReadSideBenchmarkTest(testName)

    // Return
    Future.successful(Done)
  }

  override def runReadSideBenchmark(testId: String, eventsToPersist: Int): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    executeReadSideBenchmark(testId, eventsToPersist)
  }

  private def executeReadSideBenchmark(testId: String, eventsToPersist: Int): Future[Done.type] = {
    log.info(s"$testId - Starting creation of $eventsToPersist events...")
    Source((1 to eventsToPersist).toList)
      .map(_.toString)
      .mapAsync(4) { num =>
        log.debug(s"N=$num - Initializing and sending event")
        val id = entityId(testId, num)
        entityRef(id).ask[BenchmarkConfirmation](
          CreateBenchmarkEvent(testId, eventsToPersist, readSideConfigValues.pollFrequency, readSideConfigValues.actorQueryDelay, _)
        )
      }
      .runWith(Sink.seq)
      .map(_ => Done)
      .andThen {
        case Success(n) => log.info(s"$testId - Successfully created $n events")
        case Failure(t) => log.error(s"$testId - Error creating events - ${t.getMessage}")
      }
  }

  /**
    * Execute a full benchmark test
    *
    * @param name : Name of the test to identify the results
    * @return : Done
    */
  def runFullReadSideBenchmarkTest(name: String): Future[Done.type] = {
    // 5x - 1 event
    // 5x - 10 events
    // 5x - 100 events
    // 5x - 1k events
    // 5x - 10k events
    // 1x - 100k events

    // (name, event-count, iterations)
    val tests = Seq(
      (s"$name-1", 1, 5),
      (s"$name-10", 10, 5),
      (s"$name-100", 100, 5),
      (s"$name-1k", 1000, 5),
      (s"$name-10k", 10000, 5),
      // (s"$name-100k", 100000, 1)
    )

    Source(tests)
      .mapAsync(1) {
        case (id, eventCount, iterations) =>
          log.info(s"Starting test $id for $eventCount events, will run $iterations times.")
          Source((1 to iterations).toList)
            .throttle(1, 2.second)
            .mapAsync(1) { n =>
              if (eventCount >= 10000) Thread.sleep(5000)
              val iterationId = s"$id-$n"
              executeReadSideBenchmark(iterationId, eventCount)
            }
            .runWith(Sink.seq)
            .andThen {
              case Success(n) => log.info(s"$id - Completed $iterations iterations of $eventCount events ")
              case Failure(t) => log.error(s"$id - Error running test - ${t.getMessage}", t)
            }
            .map(_ => Done)
      }
      .runWith(Sink.seq)
      .andThen {
        case Success(n) => log.info(s"$name - Completed benchmark test")
        case Failure(t) => log.error(s"$name - Error running benchmark test - ${t.getMessage}", t)
      }
      .map(_ => Done)
  }
}
