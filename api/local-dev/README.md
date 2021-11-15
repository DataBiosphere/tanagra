# local-dev
Files related to doing local development on the api service.
See the [api README](../README.md)

## Postgres
Tanagra uses a local Postgres database for some tests. To start a postgres container configured with
a database/user for testing run

To start a postgres container configured with the necessary databases:
```sh
./api/local-dev/run_postgres.sh start
```
To stop the container:
```sh
./api/local-dev/run_postgres.sh stop
```
Note that the contents of the database are not saved between container runs.
