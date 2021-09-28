# Worklog
Worklog of initial examples of building indexes.

## Flatten condition relationships
Initial flattening of SNOMED conditions in a synpuf concept set.

```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:synpuf_indexes.flatten_snomed_conditions_0 --hierarchyQuery=workflow/src/main/resources/queries/omop_snomed_condition_relations.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## copying BQ tables
Need to export postgres password and service account credentials file.
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.CopyBqTable -Dexec.args="--underlayYaml=api/src/main/resources/underlays/synpuf.yaml --cloudSqlInstanceName=broad-tanagra-dev:us-central1:tanagra-cloudsql-2cd088adcd745a91 --cloudSqlDatabaseName=indexes --cloudSqlUserName=tanagra --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com
```