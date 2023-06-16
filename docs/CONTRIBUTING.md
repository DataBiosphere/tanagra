# Contributing Code
Tanagra is currently being developed by Verily and VUMC.
The project is shared across the All Of Us and Terra partnerships.

* [Development](#development)
  * [Broad Credentials](#broad-credentials)
  * [GitHub Personal Access Token](#github-personal-access-token)
  * [Local Postgres](#local-postgres)
  * [Local MariaDB](#local-mariadb)
  * [Build And Run Tests](#build-and-run-tests)
  * [Local Server](#local-server)
  * [Adding dependencies](#adding-dependencies)
  * [Generate SQL Golden Files](#generate-sql-golden-files)
  * [Run tests without credentials](#run-tests-without-credentials)
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

### GitHub Personal Access Token
One of the dependencies for this project is pulled from GitHub Package Repository (GPR). Even though this particular 
dependency is publicly available, [GPR requires](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages)
a classic GitHub Personal Access Token (PAT) to pull it. This is GPR behavior, not specific to this repository. When
[creating your classic PAT](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic),
you only need to give it `read:packages` scope.

Then you can make the PAT available to your build via a Maven settings file: `$HOME/.m2/settings.xml`
```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <!-- Personal access token with `read:packages` scope -->
      <password>YOUR_GITHUB_CLASSIC_PAT</password>
    </server>
  </servers>
</settings>
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

### Build And Run Tests
#### MariaDB/MySQL: 
 * Stop running postgres database, if present
   * `./service/local-dev/run_postgres.sh stop`
 * Set environmental variable `DBMS` to `mariadb` to update database connection strings for tests
   * `export DBMS=mariadb`
 * Start `mariadb` database in docker container
   * `./service/local-dev/run_mariadb.sh start`
 * Run complete test suites
   * `./gradlew clean build`
 * Run specific test cases for example `CohortServiceTest`
   * `./gradlew clean service:test --tests bio.terra.tanagra.service.CohortServiceTest`
 * Stop running mariadb database
   * `./service/local-dev/run_mariadb.sh stop`
 * Unset environmental variable `DBMS`
   * `unset DBMS`

#### Postgres:
* Stop running mariadb database, if present
    * `./service/local-dev/run_mariadb.sh stop`
* Set environmental variable `DBMS` to `postgresql` to update database connection strings for tests
    * `export DBMS=postgresql`
* Start `postgres` database in docker container
    * `./service/local-dev/run_postgres.sh start`
* Run complete test suites
    * `./gradlew clean build`
* Run specific test cases for example `CohortServiceTest`
    * `./gradlew clean service:test --tests bio.terra.tanagra.service.CohortServiceTest`
* Stop running mariadb database
    * `./service/local-dev/run_postgres.sh stop`
* Unset environmental variable `DBMS`
    * `unset DBMS`

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

### Run tests without credentials
Many tests do not require credentials to run. This is useful for developers that don't have access to the testing
infrastructure. You can always run all tests, including those that require credentials, by opening a PR in this repo.

To run all tests that do not require credentials
```
./gradlew noCloudAccessRequiredTests
```

When adding a new test that does not require credentials, tag it with `@Tag("requires-cloud-access")`.

## Deployment

If you want to export datasets to GCS bucket, create bucket and configure export
properties. See TanagraExportConfiguration.java.

## Tips

For additional logging for debugging, add to property YAML (such as application.yaml):

```
# Print SQL queries
logging.level.org.springframework.jdbc.core: trace
```
