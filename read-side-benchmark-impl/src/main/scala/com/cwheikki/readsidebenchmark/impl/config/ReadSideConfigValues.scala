package com.cwheikki.readsidebenchmark.impl.config

import com.typesafe.config.Config

case class ReadSideConfigValues(pollFrequency: Int, actorQueryDelay: Int)

object ReadSideConfigValues {

  def apply(config: Config): ReadSideConfigValues = {
    val pollFrequency = readSidePollFrequency(config)
    val journalSequenceActorDelay = journalSequenceActorQueryDelay(config)
    ReadSideConfigValues(pollFrequency, journalSequenceActorDelay)
  }

  private def readSidePollFrequency(config: Config): Int = {
    // Only supports seconds (s) and millisconds (ms)
    val rawConfig = config.getString("jdbc-read-journal.refresh-interval")
    if (rawConfig.contains("ms")) {
      rawConfig.replace("ms", "").toInt
    } else {
      // Convert to ms
      (rawConfig.replace("s", "").toInt * 1000)
    }
  }

  private def journalSequenceActorQueryDelay(config: Config): Int = {
    config.getDuration("jdbc-read-journal.journal-sequence-retrieval.query-delay").toMillis.toInt
  }
}