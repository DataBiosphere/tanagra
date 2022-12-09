#!/bin/bash

export TANAGRA_DATABASE_NAME=tanagra_db
export TANAGRA_DB_INITIALIZE_ON_START=true
export TANAGRA_DB_USERNAME=dbuser
export TANAGRA_DB_PASSWORD=dbpwd

export TANAGRA_UNDERLAY_FILES=broad/aou_synthetic/expanded/aou_synthetic.json,broad/cms_synpuf/expanded/cms_synpuf.json
export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true
export TANAGRA_AUTH_IAP_GKE_JWT=false
export TANAGRA_AUTH_BEARER_TOKEN=true

./gradlew bootRun
