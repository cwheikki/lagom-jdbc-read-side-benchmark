package com.cwheikki.readsidebenchmark.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

/**
  * The read-side-benchmark service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the ReadsidebenchmarkService.
  */
trait ReadsideBenchmarkService extends Service {

  def runFullBenchmarkTest(testName: String): ServiceCall[NotUsed, Done]

  def runReadSideBenchmark(testId: String, eventsToPersist: Int): ServiceCall[NotUsed, Done]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("read-side-benchmark")
      .withCalls(
        pathCall("/api/benchmark/full-test/:testName", runFullBenchmarkTest _),
        pathCall("/api/benchmark/:testId/:eventsToPersist", runReadSideBenchmark _)
      )
      .withAutoAcl(true)
  }
}