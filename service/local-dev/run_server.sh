#!/usr/bin/env bash

usage() { echo "$0 usage flags:" && grep " .)\ #" $0; }

usage
echo

while getopts ":avstmd" arg; do
  case $arg in
    a) # Disable authentication.
      disableAuthChecks=1
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

if [[ ${useMariaDB} ]]; then
  echo "Using MariaDB for application DB."
  export TANAGRA_DB_URI=jdbc:mariadb://127.0.0.1:5432/${TANAGRA_DATABASE_NAME}
else
  echo "Using PostGres for application DB."
  export TANAGRA_DB_URI=jdbc:postgresql://127.0.0.1:5432/${TANAGRA_DATABASE_NAME}
fi

if [[ ${useVerilyUnderlays} ]]; then
  echo "Using Verily underlays."
  export TANAGRA_UNDERLAY_FILES=cmssynpuf_verily,aouSR2019q4r4_verily,sd20230831_verily,pilotsynthea2022q3_verily
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID=verily-tanagra-dev
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=verily-tanagra-dev-export-bucket
elif [[ ${useAouUnderlays} ]]; then
  echo "Using AoU test underlays."
  export TANAGRA_UNDERLAY_FILES=aou/SR2023Q3R2_local,aou/SC2023Q3R2_local
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID=broad-tanagra-dev
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=broad-tanagra-dev-bq-export
  # uncomment both lines below for test AoU Workbench access-control model
  # export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://api-dot-all-of-us-workbench-test.appspot.com
  # export TANAGRA_ACCESS_CONTROL_MODEL=AOU_WORKBENCH
elif [[ ${useSdUnderlays} ]]; then
  echo "Using sd underlay."
  export TANAGRA_UNDERLAY_FILES=sd020230831_vumc_beta
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID=sd-vumc-tanagra-test
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=sd-test-tanagra-exports
  # uncomment both lines below for sd access-control model
  # export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://sd-tanagra-test.victrvumc.org
  # export TANAGRA_ACCESS_CONTROL_MODEL=VUMC_ADMIN
else
  echo "Using Broad underlays."
  export TANAGRA_UNDERLAY_FILES=cmssynpuf_broad,aouSR2019q4r4_broad
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID=broad-tanagra-dev
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=broad-tanagra-dev-bq-export
fi

export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true
export TANAGRA_AUTH_IAP_GKE_JWT=false

if [[ ${disableAuthChecks} ]]; then
  echo "Disabling auth checks."
  export TANAGRA_AUTH_DISABLE_CHECKS=true
  export TANAGRA_AUTH_BEARER_TOKEN=false
  export TANAGRA_AUTH_IAP_GKE_JWT=false
else
  echo "Enabling auth checks. bearer-token"
  export TANAGRA_AUTH_DISABLE_CHECKS=false
  if [[ ${useAouUnderlays} ]]; then
    export TANAGRA_AUTH_BEARER_TOKEN=true
    export TANAGRA_AUTH_IAP_GKE_JWT=false
  elif [[ ${useSdUnderlays} ]]; then
    export TANAGRA_AUTH_BEARER_TOKEN=false
    export TANAGRA_AUTH_IAP_GKE_JWT=true
  fi
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



