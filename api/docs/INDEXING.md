# Indexing
Each **underlay config specifies the mapping from the source data** to Tanagra's [entity model](ENTITY_MODEL.md). 
Tanagra can query the source data directly, but **for improved performance, Tanagra generates indexed tables and queries 
them instead**. Each underlay config also specifies a data pointer where Tanagra can write indexed tables to.

**Generating the index tables is part of the deployment process**; It is not managed by the service. There is a basic
command line interface to run the indexing jobs. Currently, this CLI just uses Gradle's application plugin, so the
commands are actually Gradle commands.

(TODO: Improve this UX e.g. package a proper CLI, add service endpoints for kicking off and monitoring these jobs,
etc.).

## Underlay Config Files
There should be a separate config for each set of source data. The underlay configs are static resources (defined at
build-time) that are packaged with the JAR file in [`api/src/main/resources/config/`](src/main/resources/config/).

### Environment
The underlay configs are organized by environment, in different sub-directories of `config/` (e.g. `broad/`, `vumc/`,
`verily/`). A benefit of defining environment-specific config in the core Tanagra repo is that we can validate code
changes against all known configs, to make sure we don't break any existing deployments. A downside of this approach is
that all deployments will have to check in their config files to the core Tanagra repo. We should revisit this approach
in the future (e.g. consider allowing run-time configs); this is most expedient for now.

(TODO: Support an environment variable to indicate which environment the service is running in.)

### Directory Structure
Each underlay config consists of multiple JSON files with a specific directory structure. The top-level underlay file
contains lists of the entity and entity group file names. Tanagra looks for these files in the `entity/` and
`entitygroup/` sub-directories, respectively. The config schema allows pointers to raw SQL files to handle table
definitions that are not supported by Tanagra's simple filtering syntax. Tanagra looks for these files in the `sql/`
sub-directory. e.g.

```
underlay.json
entity/
  entity.json
entitygroup/
  entitygroup.json
sql/
  rawsql.sql
```


## Running Indexing Jobs
Before running the indexing jobs, you need to specify the underlay config files.

There are 3 steps to generating the index tables:
1. [Expand](#expand-underlay-config) the user-specified underlay config to include information from scanning the 
source data. For example, data types and UI hints.
2. [Create](#create-index-dataset) the index dataset, if it doesn't already exist.
3. [Kickoff](#kickoff-jobs) jobs for each entity and entity group.

Below you can see an [example](#omop-example) of the commands for an OMOP dataset.

### Expand Underlay Config
Set the default application credentials to a service account key file that has read access to both the source and 
index data.
```
export GOOGLE_APPLICATION_CREDENTIALS=/credentials/indexing_sa.json
```
Expand the defaults, scan the source data, and generate an expanded underlay config file that includes all this 
information. The first argument `/config/input/omop.json` is a pointer to the user-specified underlay file.
The second argument `/config/output/` is a pointer to the directory where Tanagra can write the expanded config files.
```
./gradlew api:index -Dexec.args="EXPAND_CONFIG /config/input/omop.json /config/output/"
```

### Create Index Dataset
Create a new BigQuery dataset to hold the indexed tables.
Change the location, project, and dataset name below to your own.
```
gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
bq mk --location=location project_id:dataset_id
```
(TODO: Tanagra could do this in the future. Upside is fewer steps to run indexing and maybe easier for testing.
Potential downside is more permissions needed by indexing service account.)

### Kickoff Jobs
Set the default application credentials to a service account key file that has read access to the source and read + 
write access to the index data.
```
export GOOGLE_APPLICATION_CREDENTIALS=/credentials/indexing_sa.json
```
Do a dry run of all the indexing jobs for a particular entity. This provides a sanity check that the indexing jobs
inputs, especially the SQL query inputs, are valid. This step is not required, but highly recommended to help catch
errors/bugs sooner and without running a bunch of computation.
```
./gradlew api:index -Dexec.args="INDEX_ENTITY /config/output/omop.json person DRY_RUN"
```
Now actually kick off all the indexing jobs for the entity.
```
./gradlew api:index -Dexec.args="INDEX_ENTITY /config/output/omop.json person"
```
Repeat this (dry run + actual kick off) for each entity. The jobs for one entity do not depend on those for another
entity, so you can kick them all off concurrently.

Wait until all the indexing jobs for all the entities are complete before starting the same process for entity groups.

Do a dry run of all the indexing jobs for a particular entity group.
```
./gradlew api:index -Dexec.args="INDEX_ENTITY_GROUP /config/output/omop.json condition_occurrence_person DRY_RUN"
```
Now actually kick off all the indexing jobs for the entity group.
```
./gradlew api:index -Dexec.args="INDEX_ENTITY_GROUP /config/output/omop.json condition_occurrence_person"
```
Repeat this (dry run + actual kick off) for each entity group.

(TODO: We should kick off all jobs for all entities and entity groups with a single command, rather than requiring 
separate commands for each one. I've left it separate for now to make debugging and retesting certain parts easier 
should we encounter any errors at first.)

## OMOP Example
The `aou_synthetic` dataset uses the standard OMOP schema. You can see the underlay config files defined for this
dataset in [`api/src/main/resources/config/broad/aou_synthetic/`](src/main/resources/config/broad/aou_synthetic/).

```
./gradlew api:index -Dexec.args="EXPAND_CONFIG /config/input/omop.json /config/output/"
./gradlew api:index -Dexec.args="INDEX_ENTITY /config/output/omop.json person"
./gradlew api:index -Dexec.args="INDEX_ENTITY /config/output/omop.json condition"
```

(TODO: Continue adding to this script as more indexing jobs are added.)
