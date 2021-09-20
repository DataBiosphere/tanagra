# Worklog
Worklog of initial examples of building indexes.

## Flatten condition relationships
Initial flattening of SNOMED conditions in a synpuf concept set.

```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:synpuf_indexes.flatten_snomed_conditions --hierarchyQuery=workflow/src/main/resources/queries/omop_snomed_condition_relations.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```
