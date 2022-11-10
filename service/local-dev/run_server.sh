#!/bin/bash

export TANAGRA_DATABASE_NAME=tanagra_db
export TANAGRA_DB_INITIALIZE_ON_START=true
export TANAGRA_DB_USERNAME=dbuser
export TANAGRA_DB_PASSWORD=dbpwd

export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true

./gradlew bootRun
