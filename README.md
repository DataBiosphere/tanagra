# tanagra
Repo for the Tanagra service being developed by the All of Us DRC

# Development
To get started, run `./gradlew build` to build and test.

## Local Server
`./gradlew bootRun` starts a local server
on localhost:8080. See [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
for the Swagger API page.

## Adding dependencies
We use [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html)
for building with deterministic dependencies. When adding a new dependency, use

`./gradlew dependencies --write-locks`

to regenerate the `gradle.lockfile` with the added dependencies. If this is not done, building will
error about
> Could not resolve all files for configuration...
> Resolved foo.bar:lib:version which is not part of the dependency lock state
