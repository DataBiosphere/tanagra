# Contributing Code
Tanagra is currently being developed by Verily and VUMC.
The project is shared across the All Of Us and Terra partnerships.

* [Development](#development)
  * [Broad Credentials](#broad-credentials)
  * [Local Postgres](#local-postgres)
    * [Build And Run Tests Postgres](#build-and-run-tests)
  * [Local MariaDB](#local-mariadb)
    * [Build And Run Tests MariaDB](#build-and-run-tests)
  * [Local Server](#local-server)
  * [Adding dependencies](#adding-dependencies)
  * [Generate SQL Golden Files](#generate-sql-golden-files)
* [Deployment](#deployment)
* [Tips](#tips)

## Development

### Broad Credentials
Some tests talk to the `broad-tanagra-dev` GCP project. You need permissions on this project in order to run these 
tests locally. If you can't get permissions on this project (e.g. you do not have a Broad Institute email or access to
Vault), then **you can push your code up in a PR and a GitHub action will run the tests for you**.

(TODO: Tag the tests that require these credentials, so they're easy to skip if you don't have access to this project
but still want to run the other tests locally.)

Fetch the tanagra-dev service account key file from Vault.
```
vault login -method=github token=$(cat ~/.github-token)
./pull-credentials.sh
```
If you have trouble logging into Vault, here are some troubleshooting links:
- Link your GH account to your Broad account ([https://github.broadinstitute.org/](https://github.broadinstitute.org/)).
- Authenticate to Vault using your GitHub credentials ([https://github.com/broadinstitute/dsde-toolbox#authenticating-to-vault](https://github.com/broadinstitute/dsde-toolbox#authenticating-to-vault)).

Use the key file to set the `gcloud` application default credentials.
```
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/rendered/tanagra_sa.json
```

### Local Postgres
Tests and a local server use a local Postgres database.

To start a postgres container configured with the necessary databases:
```
./service/local-dev/run_postgres.sh start
```
To stop the container:
```
./service/local-dev/run_postgres.sh stop
```
Note that the contents of the database are not saved between container runs.

To connect to the database directly:
```
PGPASSWORD=dbpwd psql postgresql://127.0.0.1:5432/tanagra_db -U dbuser
```
If you get not found errors running the above command, but the `run_postgres.sh` script calls complete successfully,
check that you don't have PostGres running twice -- e.g. once in Docker and once in a local PostGres installation.

#### Build And Run Tests Postgres
To get started, build the code and run tests:Postgres
```
./gradlew clean build
```

### Local MariaDB
Tests and a local server use a local MariaDb database.

To start a mariaDB container configured with the necessary databases:
```
./service/local-dev/run_mariadb.sh start
```
To stop the container:
```
./service/local-dev/run_mariadb.sh stop
```
Note that the contents of the database are not saved between container runs.

To connect to the database directly:
```
docker exec -it tanagra mysql -u dbuser -pdbpwd
```

#### Build And Run Tests MariaDB
To get started change database connection strings for test harness
 * Update jdbc connection string in `./service/src/test/resources/application-test.yaml`
   ```
   db:
     initialize-on-start: true
     upgrade-on-start: true
     uri: jdbc:mariadb://127.0.0.1:5432/tanagra_db
   ```
 * Update jdbc connection string in `./service/src/test/resources/application-jdbc.yaml`
   ```
   tanagra:
     jdbc:
     dataSources:
       - dataSourceId: test-data-source
         url: jdbc:mariadb://127.0.0.1:5432/tanagra_db
   ```
 * Build the code and run tests for MariaDB
   ```
   ./gradlew clean build
   ```

Before a PR can merge, it needs to pass the static analysis checks and tests. To run the checks and tests locally:
```
./gradlew clean check
```

### Local Server
* Start a local server on `localhost:8080` with Postgres
```
./service/local-dev/run_postgres.sh start
./service/local-dev/run_server.sh
```
Note: When running for Postgres, first stop mariadb container if running.

* Start a local server on `localhost:8080` with MariaDb
```
./service/local-dev/run_mariadb.sh start
./service/local-dev/run_server.sh -m
```
Note: When running for Mariadb, first stop postgres container if running.

See [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) for the Swagger API page.

You can also run the local server with authentication disabled. This is useful when testing the UI, which does not 
have a login flow yet. (We rely on IAP to handle the Google OAuth flow in our dev deployments.)

```
./service/local-dev/run_server.sh -a
```

You can also run against the Verily underlays instead of the Broad ones if using Verily credentials.

```
./service/local-dev/run_server.sh -v
```

### Adding dependencies
**UPDATE: Dependency locking has been temporarily disabled because it's causing problems for collaborators. 
Planning to debug and add this back once we've debugged the problems.**

We use [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html)
for building with deterministic dependencies. When adding a new dependency, use
```
./gradlew :dependencies --write-locks
```
to regenerate the `gradle.lockfile` with the added dependencies. If this is not done, building will
error about
> Could not resolve all files for configuration...
> 
> Resolved foo.bar:lib:version which is not part of the dependency lock state

### Generate SQL Golden Files
Some tests compare the SQL string generated by the backend code with an expected string, stored in a golden file.
An example test is `bio.terra.tanagra.aousynthethic.BrandEntityQueriesTest.generateSqlForAllBrandEntities` and
the accompanying golden file is `service/src/test/resources/aousynthetic.all-brand-entities.sql`.

To regenerate the golden files, run the tests with the `generateSqlFiles` Gradle project property set. e.g.:
```
./gradlew cleanTest service:test --tests bio.terra.tanagra.* --info -PgenerateSqlFiles=true
```

## Deployment

If you want to export datasets to GCS bucket, create bucket and configure export
properties. See TanagraExportConfiguration.java.

## Tips

For additional logging for debugging, add to property YAML (such as application.yaml):

```
# Print SQL queries
logging.level.org.springframework.jdbc.core: trace
```
