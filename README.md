# zio-todo (Scala 3 + ZIO 2)

Implementation of the TODO backend in Scala 3 using ZIO 2.1.

### Main goals

The main goals of this implementation are to show how to:

- build ZIO-based microservice, including metrics and scheduled tasks,
- integrate **Doobie** with ZIO,
- provide HTTP API using `zio-http` and `zio-json`,
- provide gRPC API using `zio-grpc` and ScalaPB,
- use testcontainers to test repositories
    - examples for both sequential and parallel test executions using single testcontainer,
- use [scala3mock Cats Effect 3 integration](https://francois.monniot.eu/scala3mock/docs/user-guide/cats) within
  `zio-test`.
