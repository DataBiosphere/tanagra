#!/usr/bin/env bash

usage() { echo "$0 usage flags:" && grep " .)\ #" $0; }

usage
echo

while getopts ":ajvstemdh" arg; do
  case $arg in
    a) # Disable authentication.
      disableAuthChecks=1
      ;;
    j) # Generic JWT
      jwt=1
      ;;
    v) # Use Verily underlays.
      useVerilyUnderlays=1
      ;;
    s) # Use sd underlays.
      useSdUnderlays=1
      ;;
    t) # Use AoU test underlays
      useAouUnderlays=1
      ;;
    e) # Use eMerge test underlays
      useEmergeUnderlays=1
      ;;
    m) # Use MariaDB.
      useMariaDB=1
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

if [[ ${useMariaDB} ]]; then
  echo "Using MariaDB for application DB."
  export TANAGRA_DB_URI=jdbc:mariadb://127.0.0.1:5432/${TANAGRA_DATABASE_NAME}
else
  echo "Using PostGres for application DB."
  export TANAGRA_DB_URI=jdbc:postgresql://127.0.0.1:5432/${TANAGRA_DATABASE_NAME}
fi

if [[ ${useVerilyUnderlays} ]]; then
  echo "Using Verily underlays."
  export TANAGRA_UNDERLAY_FILES=cmssynpuf_oneverily_dev,aouSR2019q4r4_oneverily_dev,pilotsynthea2022q3_oneverily_dev,aouSC2023Q3R2_oneverily_dev
  export TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID=prj-d-1v-ucd
  export TANAGRA_EXPORT_SHARED_BQ_DATASET_IDS=workbench_de_backend_us_dev,workbench_de_backend_us_central1_dev
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=workbench_de_backend_us_dev,workbench_de_backend_us_central1_dev
elif [[ ${useAouUnderlays} ]]; then
  echo "Using AoU test underlays."
  export TANAGRA_UNDERLAY_FILES=aou/SR2023Q3R2_local,aou/SC2023Q3R2_local
  export TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID=broad-tanagra-dev
  export TANAGRA_EXPORT_SHARED_BQ_DATASET_IDS=service_export_us,service_export_uscentral1
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=broad-tanagra-dev-bq-export,broad-tanagra-dev-bq-export-uscentral1
  # uncomment both lines below for test AoU Workbench access-control model
  # export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://api-dot-all-of-us-workbench-test.appspot.com
  # export TANAGRA_ACCESS_CONTROL_MODEL=AOU_WORKBENCH
elif [[ ${useSdUnderlays} ]]; then
  echo "Using sd underlay."
  export TANAGRA_UNDERLAY_FILES=sd/sd_local
  export TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID=sd-vumc-tanagra-test
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=sd-test-tanagra-exports
  # uncomment both lines below for sd access-control model
  # export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://vumc-cohort-builder-dev.victrvumc.org
  # export TANAGRA_ACCESS_CONTROL_MODEL=VUMC_ADMIN
elif [[ ${useEmergeUnderlays} ]]; then
  echo "Using eMerge underlay."
  export TANAGRA_UNDERLAY_FILES=emerge/emerge_local,sd/sd_local
  export TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID=vumc-emerge-dev
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=vumc-emerge-dev-exports
  # uncomment both lines below for emerge access-control model
  # export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://vumc-cohort-builder-dev.victrvumc.org
  # export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://vumc-cohort-builder-dev.victrvumc.org
  # export TANAGRA_ACCESS_CONTROL_MODEL=VUMC_ADMIN
else
  echo "Using Broad underlays."
  export TANAGRA_UNDERLAY_FILES=cmssynpuf_broad,aouSR2019q4r4_broad
  export TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID=broad-tanagra-dev
  export TANAGRA_EXPORT_SHARED_BQ_DATASET_IDS=service_export_us,service_export_uscentral1
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=broad-tanagra-dev-bq-export,broad-tanagra-dev-bq-export-uscentral1
fi

export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true

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
  # set issuer, audience and public key file if token verification is needed
  # export TANAGRA_AUTH_JWT_ISSUER=
  # export TANAGRA_AUTH_JWT_AUDIENCE=
  # export TANAGRA_AUTH_JWT_PUBLIC_KEY_FILE=
  # export TANAGRA_AUTH_JWT_ALGORITHM="RSA"
else
  echo "Enabling auth checks: gcp-access-token"
  export TANAGRA_AUTH_GCP_ACCESS_TOKEN=true
fi

echo

if [[ ${debugJvm} ]]; then
   ./gradlew service:bootRun --debug-jvm
    echo "Enabling server jvm debug"
    echo "Listening for transport dt_socket at address: 5005"
  # ./gradlew service:bootRun -Dagentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
else
  ./gradlew service:bootRun
fi



