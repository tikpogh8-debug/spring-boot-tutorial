# reactive-demo

Reactive exercise: WebFlux + Spring Data R2DBC (H2 in memory), router functions, ETL with backpressure.
Standalone module, same style as the other demos. Java 25, Spring Boot 4.1.2.

## Run

```bash
export RAKUTEN_APP_ID=<your key>   # optional; without it the ETL is skipped and the API still works
./mvnw -f reactive-demo/pom.xml spring-boot:run
./mvnw -f reactive-demo/pom.xml test
```

Rakuten URL, path, keyword, hits and max pages are in `application.yml` (`rakuten.*`).

## Task 1: ETL
- `Sport` (id, name) and `SportRepository` (`ReactiveCrudRepository`)
- `Setup` (`ApplicationRunner`): `RakutenClient` pages through the Rakuten Ichiba Item Search JSON
  (`expand` for next pages, `flatMapIterable` for items), maps each `itemName` to a `Sport` and saves it
  with the repository. Names that already exist are skipped, so it can be re-run.

## Task 2: API (router functions)
| Method | Path | Result |
|---|---|---|
| POST | `/api/v1/sport/{sportName}` | 201 with the sport, 409 if the name exists, 400 if blank |
| GET | `/api/v1/sport?q=...` | sports whose name contains `q` (case-insensitive), all if `q` is missing |

409 is decided by `existsByName` and, for concurrent requests, by the unique constraint (`DuplicateKeyException`).

## Task 3: Backpressure
`Setup.BatchSubscriber` extends `BaseSubscriber`: `request(20)` on subscribe, then `request(20)` after every 20th
`onNext`. The batch size is `etl.batch-size`. Log output:

```
request(20)
onNext #1 (batch item 1/20) ... onNext #20 (batch item 20/20)
request(20)
onNext #21 ... onNext #40
request(20)
```
`SetupTest.loadRequestsTwentyAtATime` asserts the requests are exactly `20, 20, 20` for 45 elements.
