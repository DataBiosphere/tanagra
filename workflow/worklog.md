# Worklog
Worklog of initial examples of building indexes.

## Flatten condition relationships
Initial flattening of SNOMED conditions in a synpuf concept set.

```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:synpuf_indexes.flatten_snomed_conditions_0 --hierarchyQuery=workflow/src/main/resources/queries/omop_snomed_condition_relations.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

Initial flattening of SNOMED conditions in aou synthetic:

```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.flatten_snomed_conditions_0 --hierarchyQuery=workflow/src/main/resources/queries/omop_snomed_condition_relations.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## copying BQ tables
Need to export postgres password and service account credentials file.
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.CopyBigQueryDatasetToPostgres -Dexec.args="--underlayYaml=api/src/main/resources/underlays/synpuf.yaml --cloudSqlInstanceName=broad-tanagra-dev:us-central1:tanagra-cloudsql-2cd088adcd745a91 --cloudSqlDatabaseName=indexes --cloudSqlUserName=tanagra --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"```
```

## Generate hierarchy tables for all concept domain-based entities
Fetch the tanagra-dev SA key file from Vault.
```
vault login -method=github token=$(cat ~/.github-token)
cd workflow/
./pull-credentials.sh
```
If you have trouble logging into Vault, here are some troubleshooting links:
- Link your GH account to your Broad account ([https://github.broadinstitute.org/](https://github.broadinstitute.org/)).
- Authenticate to Vault using your GH credentials ([https://github.com/broadinstitute/dsde-toolbox#authenticating-to-vault](https://github.com/broadinstitute/dsde-toolbox#authenticating-to-vault)).

Use the key file to set the `gcloud` application default credentials.
```
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/rendered/tanagra_sa.json
```

Generate the ancestor-descendant tables `concept_ancestor_descendant_1` for all concept domain-based entities in the `synpuf` and `aou_synthetic` underlays.
These commands took ~30 and ~40 minutes, respectively.
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:synpuf_indexes.concept_ancestor_descendant_1 --hierarchyQuery=workflow/src/main/resources/queries/synpuf/concept_ancestor_descendant_1.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.concept_ancestor_descendant_1 --hierarchyQuery=workflow/src/main/resources/queries/aou_synthetic/concept_ancestor_descendant_1.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```
