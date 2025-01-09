#!/usr/bin/env bash

usage() { echo "$0 usage flags:" && grep " .)\ #" "${0}"; }

while getopts ":ajdh" arg; do
  case $arg in
    a) # Disable authentication.
      disableAuthChecks=1
      ;;
    j) # Generic JWT
      jwt=1
      ;;
    d) # enable debug-jvm for bootRun.
      debugJvm=1
      ;;
    h | *) # Display help.
      usage
      exit 0
      ;;
  esac
done

export TANAGRA_DATABASE_NAME=tanagra_db
export TANAGRA_DB_INITIALIZE_ON_START=false
export TANAGRA_DB_USERNAME=dbuser
export TANAGRA_DB_PASSWORD=dbpwd
export TANAGRA_FEATURE_MAX_CHILD_THREADS= # Set to 0 to run everything serially.

echo "... Starting local postgres application database ..."
export TANAGRA_DB_URI=jdbc:postgresql://127.0.0.1:5432/${TANAGRA_DATABASE_NAME}

echo "... Using Verily underlays from dev-stable ..."
export TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID=prj-d-1v-ucd
export TANAGRA_EXPORT_SHARED_BQ_DATASET_IDS=workbench_de_backend_us_dev,workbench_de_backend_us_central1_dev
export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=workbench_de_backend_us_dev,workbench_de_backend_us_central1_dev

export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true

export TANAGRA_ACCESS_CONTROL_MODEL=bio.terra.tanagra.service.accesscontrol.model.impl.SamResourceAccessControl
export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://workbench-dev.verily.com/api/sam

# UnderlayName --------- Config file -------------------- Data collection UUID
# aouSR2019q4r4          aouSR2019q4r4_oneverily_dev      ca710798-8c38-4807-9b81-be494e7ac7f2
# cmssynpuf              cmssynpuf_oneverily_dev          f8eab6af-7fed-49c8-b6a3-433a9dee75bb
# pilotsynthea2022q3     pilotsynthea2022q3_oneverily_dev 69fc524f-8141-42b4-85c7-14e58e9b6f1c
# aouSC2023Q3R2_testonly aouSC2023Q3R2_oneverily_dev      2d24cc89-3dc1-4a30-b147-deaaaef336d5

export TANAGRA_UNDERLAY_FILES=cmssynpuf_oneverily_dev,aouSR2019q4r4_oneverily_dev,pilotsynthea2022q3_oneverily_dev,aouSC2023Q3R2_oneverily_dev
export TANAGRA_ACCESS_CONTROL_PARAMS=aouSR2019q4r4,ca710798-8c38-4807-9b81-be494e7ac7f2,cmssynpuf,f8eab6af-7fed-49c8-b6a3-433a9dee75bb,pilotsynthea2022q3,69fc524f-8141-42b4-85c7-14e58e9b6f1c,aouSC2023Q3R2_testonly,2d24cc89-3dc1-4a30-b147-deaaaef336d5

# Set defaults
export TANAGRA_AUTH_DISABLE_CHECKS=false
export TANAGRA_AUTH_IAP_GKE_JWT=false
export TANAGRA_AUTH_IAP_APP_ENGINE_JWT=false
export TANAGRA_AUTH_GCP_ACCESS_TOKEN=false
export TANAGRA_AUTH_JWT=false

if [[ ${disableAuthChecks} ]]; then
  echo "Disabling auth checks."
  export TANAGRA_AUTH_DISABLE_CHECKS=true
elif [[ ${jwt} ]]; then
  echo "Enabling auth checks: jwt"
  export TANAGRA_AUTH_JWT=true
else
  echo "Enabling auth checks: gcp-access-token"
  export TANAGRA_AUTH_GCP_ACCESS_TOKEN=true
fi

if [[ ${debugJvm} ]]; then
   ./gradlew service:bootRun --debug-jvm
    echo "... Enabling server jvm debug ..."
    echo "Listening for transport dt_socket at address: 5005"
    # ./gradlew service:bootRun -Dagentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
else
  echo "... Starting server ..."
  ./gradlew service:bootRun
fi
