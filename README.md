### Installation
##### CE2
```sbt
libraryDependencies += "com.bcf" %% "reactivemongo4s" % "0.1.0"
```
##### CE3
```sbt
libraryDependencies += "com.bcf" %% "reactivemongo4s" % "0.2.0"
```

### Usage
##### Examples
```scala
import com.bcf.reactivemongo4s.implicits._

val col: BSONCollection = ???

override def run(args: List[String]): IO[ExitCode] =
    for {
    res <- col.find(BSONDocument("b" -> BSONDocument("$exists" -> true)))
      .cursor[SomeModel]()
      .toStream[IO](100)
    _ <- res.evalMap(model => IO.println(s"Got $model")).compile.drain
    count <- col.countF[IO]
    _ <- IO.println("Count: " + count)
    countAggregated <- col.aggregateWith[SomeModel]() { framework =>
      import framework.{Count, Match}
    
      List(
        Match(BSONDocument("b" -> BSONDocument("$gte" -> 1000))),
        Count("total")
      )
    }.headF[IO]
    _ <- IO.println("countAggregated: " + countAggregated)
    } yield ()
```

### Development

##### Run dockerized mongo

```bash
$ docker network create mongo4cats
$ docker run --name mongo4cats --network mongo4cats -p 27017 -d mongo:latest
```

##### Populate test db

Create two collections for testing:
- `test1` of size 1000
- `test2` of size 20

```bash
$ docker run -it --network mongo4cats --rm mongo mongo --host mongo4cats test
> use test
> let i = 0
> while (i < 1000) { db.test1.insertOne({"b": i}); i++ }
> let j = 0
> while (j < 20) { db.test2.insertOne({"b": j}); j++ }
> exit
```