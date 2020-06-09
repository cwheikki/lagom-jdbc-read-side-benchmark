# Lagom JDBC read-side benchmark

A Lagom application to benchmark the total time that it takes from sending a command to a persistent entity to the read-side being updated such that the user would observe the change when querying the database.

### To run:

```
> sbt runAll

> curl http://localhost:9000/api/benchmark/full-test/full-test-1
```

### Configurations under test
These are the configurations that are being tested and adjusted for each benchmark run.

`jdbc-read-journal.refresh-interval` - The interval at which the [JdbcReadJournal](https://github.com/akka/akka-persistence-jdbc/blob/afdcea24e946247f8ed8e3306ddd49e395418d25/core/src/main/scala/akka/persistence/jdbc/query/scaladsl/JdbcReadJournal.scala#L182) will fetch new events from the journal

`jdbc-read-journal.journal-sequence-retrieval.query-delay` - The interval at which the [JournalSequenceActor](https://github.com/akka/akka-persistence-jdbc/blob/7930dc327455f0de37614772424cabcfa5d22099/core/src/main/scala/akka/persistence/jdbc/query/JournalSequenceActor.scala#L59) will query and refresh the max ordering value
This configuration is important, because the `JdbcReadJournal` asks this actor for the `maxOrderingId` which is then used to [fetch the events from the journal](https://github.com/akka/akka-persistence-jdbc/blob/afdcea24e946247f8ed8e3306ddd49e395418d25/core/src/main/scala/akka/persistence/jdbc/query/scaladsl/JdbcReadJournal.scala#L237). No matter how often the JdbcReadJournal refreshes, if the `maxOrderingId` isn't updated then new events will not be queried. 

Because of the relationship between the `JdbcReadJournal` and the `JournalSequenceActor`, it is very important to configure both of these values together in order to get the performance benefits.

### Benchmark results

Test Run | Refresh Interval Config(ms) | Query Delay Config(ms) | Events Persisted | Min update time(ms) | Max update time(ms) | Avg update time(ms) |
------|----|----|------|------|------|------|
test-1|1000 (default)|1000 (default)|1|219|1041|431.4
test-10|1000 (default)|1000 (default)|10|302|499|388.8
test-100|1000 (default)|1000 (default)|100|321|704|521.19
test-1k|1000 (default)|1000 (default)|1000|290|1640|916.67
test-10k|1000 (default)|1000 (default)|10000|53|2334|1399.88
test-100k|1000 (default)|1000 (default)|100000|181|2587|1479.94
test-1|500|500|1|214|654|422.8
test-10|500|500|10|273|626|439.48
test-100|500|500|100|259|814|541.86
test-1k|500|500|1000|83|1087|583.79
test-10k|500|500|10000|69|1175|692.23
test-100k|500|500|100000|134|1267|723.44
test-1|200|200|1|183|397|266.4
test-10|200|200|10|251|393|325.04
test-100|200|200|100|19|450|249.09
test-1k|200|200|1000|30|598|346.61
test-10k|200|200|10000|49|574|310.52
test-100k|200|200|100000|65|772|351.39
test-1|100|100|1|84|175|131
test-10|100|100|10|39|162|102
test-100|100|100|100|33|227|140.41
test-1k|100|100|1000|33|267|158.17
test-10k|100|100|10000|22|328|168.09
test-100k|100|100|100000|39|712|189
test-1|50|50|1|33|134|66.8
test-10|50|50|10|16|125|64.28
test-100|50|50|100|18|146|88.06
test-1k|50|50|1000|23|171|98.42
test-10k|50|50|10000|24|226|106.05
test-100k|50|50|100000|27|210|108.12

#### Querying results after running benchmark
```sql
select
    m.test_id,
    max(m.refresh_interval_ms) as refresh_interval_ms,
    max(m.query_delay_ms) as query_delay_ms,
    max(m.total_events_persisted) as total_events_persisted,
    min(m.millis) as min_read_side_update_time_millis,
    max(m.millis) as max_read_side_update_time_millis,
    round(avg(m.millis), 2) as avg_read_side_update_time_millis
from (select *,
             (time_to_update_ns / 1000000) as millis
      from read_side_benchmark) m
where m.test_id like 'test-1%'
group by (test_id, refresh_interval_ms, query_delay_ms)
order by total_events_persisted asc;
```

### Future enhancements
- Metrics/monitoring to understand impact on application CPU
- Metrics/monitoring to understand impact on database