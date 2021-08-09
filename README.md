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