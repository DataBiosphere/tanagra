# Developer Information
The information below mostly concerns the backend parts of the codebase (underlay config, service, indexer).
UI-specific information [lives here](./UI.md).

## Build and run tests

### GitHub credentials
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

### Build code
```
./gradlew clean compileJava
```

### GCP credentials for test project
Some tests require GCP resources that live in the `broad-tanagra-dev` GCP project. You need credentials that have
read/write/query access to this project to run these tests locally. There are 2 ways to get these credentials.

1. (Recommended) Use the same service account that this repo's GitHub actions use.
Download the tanagra-dev service account key file from Vault.
```
vault login -method=github token=$(cat ~/.github-token)
./pull-credentials.sh
```
If you cannot log in into Vault, here are some troubleshooting links:
- Link your GitHub account to your Broad account ([https://github.broadinstitute.org/](https://github.broadinstitute.org/)).
- Authenticate to Vault using your GitHub credentials ([https://github.com/broadinstitute/dsde-toolbox#authenticating-to-vault](https://github.com/broadinstitute/dsde-toolbox#authenticating-to-vault)).

Use the key file to set the `gcloud` application default credentials.
```
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/rendered/tanagra_sa.json
```

2. Add your end-user Google account to the `tanagra-team-dev@pmi-ops.org` Google group.
Members of this Google group have the required permissions. Set the application default credentials to your own:
```
gcloud auth application-default login
```

#### If you can't get GCP credentials
If you can't get GCP credentials, you can open a PR with your changes and this repo's GitHub actions will pull
the appropriate credentials and run all the tests for you.

You can also run just the subset of the [tests that don't require GCP credentials](#run-tests).

### Start application database
Some tests in the `service` Gradle subproject require a running application database (PostGres, MySql, or MariaDB).
You can run this locally by either installing the database directly, or (recommended) using Docker.

On a Mac, you can use Docker Desktop or Colima to run a Docker daemon. To start Colima now and restart at every login:
```
brew services start colima
```
If you don't want/need a background service you can just run:
```
/opt/homebrew/opt/colima/bin/colima start -f
```

Run a script to start/stop and initialize the application database.
- For PostGres: 
```
export DBMS=postgresql
./service/local-dev/run_postgres.sh start
./service/local-dev/run_postgres.sh stop
```
- For MariaDB:
```
export DBMS=mariadb
./service/local-dev/run_mariadb.sh start
./service/local-dev/run_mariadb.sh stop
```
If you encounter port conflicts, try shutting down any existing application databases and re-starting.
Note that the contents of the database are not saved between container runs.

You can connect to the application database directly (e.g. for debugging).
- For PostGres: `PGPASSWORD=dbpwd psql postgresql://127.0.0.1:5432/tanagra_db -U dbuser`
- For MariaDB: `docker exec -it tanagra mysql -u dbuser -pdbpwd`

### Run tests
- All tests: `./gradlew clean check`
- Single test: `./gradlew clean service:test --tests bio.terra.tanagra.service.CohortServiceTest`
- Only tests that don't require GCP credentials: `./gradlew noCloudAccessRequiredTests`.
  When adding a new test that does not require credentials, tag it with `@Tag("requires-cloud-access")`.

## Deploy locally
1. Set the application default credentials to something with read/query access to the underlays you want to serve.
    - For a service account key file: `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service/account/key/file`
    - For end-user credentials: `gcloud auth application-default login`
2. [Start the application database](#start-application-database).
3. Start the service on `localhost:8080`. e.g.
    ```
    ./service/local-dev/run_server.sh -a
    ```
    Run the script without any flags to see the list of available options.
    
    Go to [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) for the Swagger UI.
4. Start the UI.
    ```
    npm run codegen
    npm install
    npm start
    ```
When testing the UI, make sure to disable authentication checks with the `run_server.sh -a` flag.
Our deployments rely on IAP to handle the Google OAuth flow, so there is no login flow built into the UI.

## Static analysis
We use Checkstyle, Spotbugs, and PMD for static analysis checks.
These checks are included in the `./gradlew check` task and get run for every PR.
You can run just the static analysis checks (i.e. no functional tests) with:
```
./gradlew spotlessApply checkstyleMain checkstyleTest spotbugsMain spotbugsTest pmdMain pmdTest
```

## Dependency locking
We use [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html)
for building with deterministic dependencies. When adding a new dependency, use
```
./gradlew underlay:dependencies service:dependencies cli:dependencies indexer:dependencies annotationProcessor:dependencies --write-locks
```
to regenerate the `gradle.lockfile` for each subproject with the added dependencies. Outdated lock files cause errors like:
```
> Could not resolve all files for configuration...
>
> Resolved foo.bar:lib:version which is not part of the dependency lock state
```

## SQL golden files
Some tests compare the SQL generated by the query engine with an expected string, saved in a golden file.
e.g. [`BQCountQueryTest`](../underlay/src/test/java/bio/terra/tanagra/query/bigquery/sqlbuilding/BQCountQueryTest.java)

To regenerate the golden files, run the tests with the `generateSqlFiles` Gradle project property set to true.
```
./gradlew underlay:test -PgenerateSqlFiles=true
```

## Logging level
Modify the logging level for the service directly in the [`application.yml` file](../service/src/main/resources/application.yml). e.g.
```
logging.level.org.springframework.jdbc.core: trace
```

## Generated documentation
We generate documentation for the underlay and service application configuration properties, directly from the code.
Run the `checkGeneratedFiles.sh` script to regenerate the documentation or confirm that no changes are needed.

```
.github/tools/checkGeneratedFiles.sh
```

If any changes are generated, check them in. This script is also run in a GitHub action that will prevent you from
merging code that has code inconsistent documentation.

## General coding guidelines
AN easy way to inspect and cleanup code during development is using IntelliJ's 'Analyze Code' feature. 
More information on their website. See [Analyze code before committing it to Git](https://www.jetbrains.com/help/idea/running-inspections.html#run-before-commit). 
Performance optimizations, possible null pointer exceptions and other code issues are listed along with suggested fixes.

- [Google Shell Style Guide](https://google.github.io/styleguide/shellguide.html)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
