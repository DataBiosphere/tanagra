# Indexing
Tanagra can query the source data directly, but **for improved performance, Tanagra generates indexed tables and queries 
them instead**. The indexer config specifies where Tanagra can write generated index tables.

**Generating index tables is part of the deployment process**; It is not managed by the service. There is a basic
command line interface to run the indexing jobs. Currently, this CLI just uses Gradle's application plugin, so the
commands are actually Gradle commands.

## Running Indexing Jobs
Before running the indexing jobs, you need to specify the data mapping and indexer [config files](CONFIG_FILES.md).

There are 4 steps to generating the index tables:
1. [Setup](#setup-credentials) credentials with read permissions on the source data, and read-write permissions on 
the index data.
2. [Create](#create-index-dataset) the index dataset.
3. [Build](#build-code) the indexer code.
4. [Run](#run-jobs) the jobs.

Below you can see an [example](#omop-example) of the commands for an OMOP dataset.

### Setup Credentials
You need to set up 2 types of credentials to run all parts of indexing:
1. Default application credentials to allow Tanagra to talk to BigQuery.
2. gcloud credentials to allow you to create a new BigQuery dataset using the `bq` CLI.

#### Default Application Credentials
Set the default application credentials to an account that has read permissions on the source data, and read-write 
permissions on the index data. Best practice is to use a service account that has indexing-specific permissions, but
for debugging, end-user credentials can be very useful.

To use a service account key file, you can set an environment variable:
```
export GOOGLE_APPLICATION_CREDENTIALS=$(PWD)/rendered/tanagra_sa.json
```

To use end-user credentials, you can run:
```
gcloud auth application-default login
```
More information and ways to set up application default credentials in 
[the GCP docs](https://cloud.google.com/docs/authentication/provide-credentials-adc).

#### gcloud Credentials
To use a service account key file:
```
gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
```

To use end-user credentials:
```
gcloud auth login
```

### Create Index Dataset
Create a new BigQuery dataset to hold the indexed tables.
Change the location, project, and dataset name below to your own.
```
bq mk --location=location project_id:dataset_id
```

### Build Code
Build the indexer code once.
```
source indexer/tools/local-dev.sh
```

If you make a change to any config file, no need to re-build.
If you make a change to any Java code, you should re-build.

### Run Jobs

#### Available Commands
Documentation for the indexing commands is generated from annotations in the Java classes as 
a [manpage](generated/indexer-cli/tanagra.adoc).
You can also see usage help for the commands by omitting arguments. e.g.
```
> tanagra
Tanagra command-line interface.
Usage: tanagra [COMMAND]
Commands:
  index  Commands to run indexing.
  clean  Commands to clean up indexing outputs.
Exit codes:
  0     Successful program execution
  1     User-actionable error (e.g. missing parameter)
  2     System or internal error (e.g. validation error from within the Tanagra
         code)
  3     Unexpected error (e.g. Java exception)
```

#### Dry Run
Do a dry run first for validation. This provides a sanity check that the indexing jobs inputs, especially the SQL
query inputs, are valid. This step is not required, but highly recommended to help catch errors/bugs sooner and without
running a bunch of computation first. Dry run includes validating that the attribute data types specified match those
returned by running the SQL query.
```
tanagra index underlay --indexer-config=cmssynpuf_verily --dry-run
```

#### All Jobs
Kick off all jobs for the underlay.
```
tanagra index underlay --indexer-config=cmssynpuf_verily
```
This can take a long time to complete. If e.g. your computer falls asleep, or you need to kill the process on your
computer, you can re-run the same command again. You need to check that there are no in-progress Dataflow jobs in the
project before kicking it off again, because the jobs check for the existence of the output BQ table (not whether there
are any in-progress Dataflow jobs) to tell if they need to run.

#### Jobs for Entity/Group
You can also kick off the jobs for a single entity or entity group. This is helpful for testing and debugging.
To kick off all the jobs for a single entity:
```
tanagra index entity --names=person --indexer-config=cmssynpuf_verily --dry-run
tanagra index entity --names=person --indexer-config=cmssynpuf_verily
```
all entities:
```
tanagra index entity --all --indexer-config=cmssynpuf_verily --dry-run
tanagra index entity --all --indexer-config=cmssynpuf_verily
```
a single entity group:
```
tanagra index group --names=conditionPerson --indexer-config=cmssynpuf_verily --dry-run
tanagra index group --names=conditionPerson --indexer-config=cmssynpuf_verily
```
all entity groups:
```
tanagra index group --all --indexer-config=cmssynpuf_verily --dry-run
tanagra index group --all --indexer-config=cmssynpuf_verily
```

All the entities in a group must be indexed before the group. The `tanagra index underlay` command ensures this ordering, 
but keep this in mind if you're running the jobs for each entity or entity group separately.

## Troubleshooting

### Concurrency
By default, the indexing jobs are run concurrently as much as possible. You can force it to run jobs serially by
overriding the default job executor:
```
tanagra index underlay --indexer-config=cmssynpuf_verily --job-executor=SERIAL
```

### Re-Run Jobs
Indexing jobs will not overwrite existing index tables. If you want to re-run indexing, either for a single entity/group 
or for everything, you need to delete any existing index tables. You can either do that manually or using the clean
commands below. Similar to the indexing commands, the clean commands also allow dry runs.

To clean the generated index tables for everything:
```
tanagra clean underlay --indexer-config=cmssynpuf_verily --dry-run
tanagra clean underlay --indexer-config=cmssynpuf_verily
```
a single entity:
```
tanagra clean entity --names=person --indexer-config=cmssynpuf_verily --dry-run
tanagra clean entity --names=person --indexer-config=cmssynpuf_verily
```
a single entity group:
```
tanagra clean group --names=conditionPerson --indexer-config=cmssynpuf_verily --dry-run
tanagra clean group --names=conditionPerson --indexer-config=cmssynpuf_verily
```

### Run Dataflow Locally
While developing a job, running locally is faster. Also, you can use Intellij debugger.

Add to `BigQueryIndexingJob.buildDataflowPipelineOptions()`:
  ```
  import org.apache.beam.runners.direct.DirectRunner;
  
  dataflowOptions.setRunner(DirectRunner.class);
  dataflowOptions.setTempLocation("gs://dataflow-staging-us-central1-694046000181/temp");
  ```

### Filter On One Person
Filter your queries on one person, eg:
  ```
  .where(
      new BinaryFilterVariable(
          idFieldVar,
          BinaryOperator.EQUALS,
          new Literal.Builder().dataType(DataType.INT64).int64Val(1107050).build()))
  ```

## OMOP Example
The `cmssynpuf` underlay is a data mapping for a [public dataset](https://console.cloud.google.com/marketplace/product/hhs/synpuf) 
that uses the OMOP schema.

You can see the top-level underlay config file for this dataset [here](../underlay/src/main/resources/config/underlay/cmssynpuf/underlay.json).

```
bq mk --location=US verily-tanagra-dev:cmssynpuf_index

tanagra index underlay --indexer-config=cmssynpuf_verily --dry-run
tanagra index underlay --indexer-config=cmssynpuf_verily
```
