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
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/workflow/rendered/tanagra_sa.json
```

Generate the ancestor-descendant tables `concept_ancestor_descendant_1` for all concept domain-based entities in the `synpuf` and `aou_synthetic` underlays.
These commands took ~30 and ~40 minutes, respectively.
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:synpuf_indexes.concept_ancestor_descendant_1 --hierarchyQuery=workflow/src/main/resources/queries/synpuf/concept_ancestor_descendant_1.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.concept_ancestor_descendant_1 --hierarchyQuery=workflow/src/main/resources/queries/aou_synthetic/concept_ancestor_descendant_1.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

Generate the node-path tables `concept_node_path_1` for all concept domain-based entities in the `synpuf` and `aou_synthetic` underlays.
These commands took ~50 and ~20 minutes, respectively.
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:synpuf_indexes.concept_node_path_1 --hierarchyQuery=workflow/src/main/resources/queries/synpuf/concept_ancestor_descendant_1.sql --allNodesQuery=workflow/src/main/resources/queries/synpuf/concept_node_path_allnodes_1.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.concept_node_path_1 --hierarchyQuery=workflow/src/main/resources/queries/aou_synthetic/concept_ancestor_descendant_1.sql --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/concept_node_path_allnodes_1.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## Generate (node,path) tables for the CONDITION, PROCEDURE, MEASUREMENT, INGREDIENT entities
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_node_path_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/condition_parentchild_2.sql --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/condition_allnodes_2.sql --rootNodesFilterQuery=workflow/src/main/resources/queries/aou_synthetic/condition_rootnodesfilter_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.procedure_node_path_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_parentchild_2.sql --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_allnodes_2.sql --rootNodesFilterQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_rootnodesfilter_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.measurement_node_path_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_parentchild_2.sql --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_allnodes_2.sql --rootNodesFilterQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_rootnodesfilter_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.ingredient_node_path_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_parentchild_2.sql --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_allnodes_2.sql --rootNodesFilterQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_rootnodesfilter_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## Generate (parent,child) tables for the CONDITION, PROCEDURE, MEASUREMENT, INGREDIENT entities
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteParentChildRelationships -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_parent_child_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/condition_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteParentChildRelationships -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.procedure_parent_child_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteParentChildRelationships -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.measurement_parent_child_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteParentChildRelationships -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.ingredient_parent_child_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## Generate all-(node) tables for the CONDITION, PROCEDURE, MEASUREMENT, INGREDIENT entities
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_all_nodes_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/condition_allnodes_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.procedure_all_nodes_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_allnodes_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.measurement_all_nodes_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_allnodes_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.ingredient_all_nodes_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_allnodes_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## Generate (ancestor, descendant) tables for the CONDITION, PROCEDURE, MEASUREMENT, INGREDIENT entities
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_ancestor_descendant_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/condition_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.procedure_ancestor_descendant_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.measurement_ancestor_descendant_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.ingredient_ancestor_descendant_2 --parentChildQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_parentchild_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## Generate (node, fullText) tables for the CONDITION, PROCEDURE, MEASUREMENT, INGREDIENT, BRAND, DEVICE, OBSERVATION, VISIT entities
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/condition_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/condition_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.procedure_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.measurement_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/measurement_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.ingredient_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/ingredient_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.brand_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/brand_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/brand_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.device_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/device_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/device_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.observation_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/observation_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/observation_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.visit_text_search_2 --allNodesQuery=workflow/src/main/resources/queries/aou_synthetic/visit_allnodes_2.sql --searchStringsQuery=workflow/src/main/resources/queries/aou_synthetic/visit_textsearch_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```

## Generate (node, count) tables for the CONDITION, PROCEDURE, MEASUREMENT, INGREDIENT, DEVICE, OBSERVATION, VISIT entities
```
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_person_counts_2 --allPrimaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/condition_allnodes_2.sql --allAuxiliaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/conditionoccurrence_allnodes_2.sql --ancestorDescendantRelationshipsQuery=workflow/src/main/resources/queries/aou_synthetic/condition_ancestordescendant_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.procedure_person_counts_2 --allPrimaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_allnodes_2.sql --allAuxiliaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/procedureoccurrence_allnodes_2.sql --ancestorDescendantRelationshipsQuery=workflow/src/main/resources/queries/aou_synthetic/procedure_ancestordescendant_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.device_person_counts_2 --allPrimaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/device_allnodes_2.sql --allAuxiliaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/deviceoccurrence_allnodes_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.observation_person_counts_2 --allPrimaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/observation_allnodes_2.sql --allAuxiliaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/observationoccurrence_allnodes_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts -Dexec.args="--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.visit_person_counts_2 --allPrimaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/visit_allnodes_2.sql --allAuxiliaryNodesQuery=workflow/src/main/resources/queries/aou_synthetic/visitoccurrence_allnodes_2.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"
```