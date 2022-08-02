# condition: DenormalizeAllNodes
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args="--outputBigQueryTable=verily-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition --allNodesQuery=condition_selectAll.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"

# person: DenormalizeAllNodes
./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args="--outputBigQueryTable=verily-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.person --allNodesQuery=person_selectAll.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com"

