# Indexing
* [Overview](#overview)
* [Running Indexing Jobs](#running-indexing-jobs)
    * [Expand Underlay Config](#expand-underlay-config)
    * [Create Index Dataset](#create-index-dataset)
    * [Kickoff Jobs](#kickoff-jobs)

## Overview
Each underlay config specifies the mapping from the source data to Tanagra's entity model. Tanagra can query the
source data directly, but for improved performance, Tanagra generates indexed tables and queries those instead.
Each underlay config also specifies a data pointer where Tanagra can write indexed tables to.

Generating the index tables is part of the deployment process; It is not managed by the service. There is a basic
command line interface to run the indexing jobs. Currently, this CLI just uses Gradle's application plugin, so the
commands are actually Gradle commands. In the future, we should improve this UX (e.g. package a proper CLI, add 
service endpoints for kicking off and monitoring these jobs, etc.).

## Running Indexing Jobs
There are 3 steps to generating the index tables:
1. Expand the user-specified underlay config to include information from scanning the source data. For example,
data types and UI hints.
2. Create the index dataset, if it doesn't already exist.
3. Kickoff jobs for each entity and entity group.

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
bq mk --location=us-central1 project_id:dataset_id
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

(TODO: We should kick off all jobs for all entities and entity groups with a single command, rather than requiring 
separate commands for each one. I've left it separate for now to make debugging and retesting certain parts easier 
should we encounter any errors at first.)
