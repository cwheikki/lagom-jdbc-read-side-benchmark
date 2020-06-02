# Lagom JDBC read-side benchmark

A Lagom application to benchmark the total time that it takes from sending a command to a persistent entity to the read-side being updated such that the user can observe the change.

### To run:

```
curl http://localhost:9000/api/benchmark/full-test/full-test-1
```