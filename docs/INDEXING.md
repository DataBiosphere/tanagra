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
build-time) that are packaged with the JAR file in [`service/src/main/resources/config/`](../service/src/main/resources/config/).

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
3. [Kickoff](#kickoff-jobs) the jobs.

Below you can see an [example](#omop-example) of the commands for an OMOP dataset.

### Expand Underlay Config
Set the default application credentials to a service account key file that has read access to both the source and 
index data.
```
export GOOGLE_APPLICATION_CREDENTIALS=$(PWD)/rendered/tanagra_sa.json
```
Expand the defaults, scan the source data, and generate an expanded underlay config file that includes all this 
information. The first argument is a pointer to the user-specified underlay file.
The second argument is a pointer to the directory where Tanagra can write the expanded config files.
Both arguments must be absolute paths. Example:
```
./gradlew indexer:index -Dexec.args="EXPAND_CONFIG $HOME/tanagra/service/src/main/resources/config/broad/cms_synpuf/original/cms_synpuf.json $HOME/tanagra/service/src/main/resources/config/broad/cms_synpuf/expanded"
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

#### All Jobs
Do a dry run of all the indexing jobs. This provides a sanity check that the indexing jobs inputs, especially the SQL 
query inputs, are valid. This step is not required, but highly recommended to help catch errors/bugs sooner and without 
running a bunch of computation first.
```
./gradlew indexer:index -Dexec.args="INDEX_ALL /config/output/omop.json DRY_RUN"
```
Now actually kick off all the indexing jobs.
```
./gradlew indexer:index -Dexec.args="INDEX_ALL /config/output/omop.json"
```
This can take a long time to complete. If e.g. your computer falls asleep or you need to kill the process on your
computer, you can re-run the same command again. You need to check that there are no in-progress Dataflow jobs in the
project before kicking it off again, because the jobs check for the existence of the output BQ table to tell if they
need to run.

TODO: Cache the Dataflow job id somewhere so we can do a more accurate check for jobs that are still running before
kicking them off again.

#### Jobs for One Entity/Group
You can also kickoff the indexing jobs for a single entity or entity group. This is helpful for testing and debugging.
To kick off all the indexing jobs for a particular entity:
```
./gradlew indexer:index -Dexec.args="INDEX_ENTITY /config/output/omop.json person DRY_RUN"
./gradlew indexer:index -Dexec.args="INDEX_ENTITY /config/output/omop.json person"
```
or entity group:
```
./gradlew indexer:index -Dexec.args="INDEX_ENTITY_GROUP /config/output/omop.json condition_occurrence_person DRY_RUN"
./gradlew indexer:index -Dexec.args="INDEX_ENTITY_GROUP /config/output/omop.json condition_occurrence_person"
```
All the entities in a group should be indexed before the group. The `INDEX_ALL` command ensures this ordering, but keep 
this in  mind if you're running the jobs for each entity or entity group separately.

#### Concurrency
By default, the indexing jobs are run concurrently as much as possible. You can force it to run jobs serially by
appending `SERIAL` to the command:
```
./gradlew indexer:index -Dexec.args="INDEX_ALL /config/output/omop.json DRY_RUN SERIAL"
./gradlew indexer:index -Dexec.args="INDEX_ALL /config/output/omop.json NOT_DRY_RUN SERIAL"
```

## OMOP Example
The `cms_synpuf` is a [public dataset](https://console.cloud.google.com/marketplace/product/hhs/synpuf) that uses the 
standard OMOP schema.

You can see the underlay config files defined for this dataset in 
[`service/src/main/resources/config/broad/cms_synpuf/`](../service/src/main/resources/config/broad/cms_synpuf/).
Note that while the source dataset is public, the index dataset that Tanagra generates is not.

```
export INPUT_DIR=$HOME/tanagra/service/src/main/resources/config/broad/cms_synpuf/original
export OUTPUT_DIR=$HOME/tanagra/service/src/main/resources/config/broad/cms_synpuf/expanded

./gradlew indexer:index -Dexec.args="EXPAND_CONFIG $INPUT_DIR/cms_synpuf.json $OUTPUT_DIR/"

bq mk --location=US broad-tanagra-dev:cmssynpuf_index

./gradlew indexer:index -Dexec.args="INDEX_ALL $OUTPUT_DIR/cms_synpuf.json DRY_RUN"
./gradlew indexer:index -Dexec.args="INDEX_ALL $OUTPUT_DIR/cms_synpuf.json"
```
