#!/usr/bin/env bash

usage() { echo "$0 usage flags:" && grep " .)\ #" $0; }

usage
echo

while getopts ":avtmd" arg; do
  case $arg in
    a) # Disable authentication.
      disableAuthChecks=1
      ;;
    v) # Use Verily underlays.
      useVerilyUnderlays=1
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
  export TANAGRA_UNDERLAY_FILES=verily/cms_synpuf/cms_synpuf.json,verily/sdd_refresh0323/sdd_refresh0323.json,verily/pilot_synthea_2022q3/pilot_synthea_2022q3.json
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID=verily-tanagra-dev
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=verily-tanagra-dev-export-bucket
elif [[ ${useAouUnderlays} ]]; then
  echo "Using AoU test underlays."
  export TANAGRA_UNDERLAY_FILES=aou/test/SC2023Q3R1/SC2023Q3R1.json,aou/test/SR2023Q3R1/SR2023Q3R1.json
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID=broad-tanagra-dev
  export TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES=broad-tanagra-dev-bq-export
  # specify access-control
  export TANAGRA_ACCESS_CONTROL_BASE_PATH=https://all-of-us-workbench-test.appspot.com
  export TANAGRA_ACCESS_CONTROL_MODEL=AOU_WORKBENCH
else
  echo "Using Broad underlays."
  export TANAGRA_UNDERLAY_FILES=broad/aou_synthetic/aou_synthetic.json,broad/cms_synpuf/cms_synpuf.json
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
  export TANAGRA_AUTH_BEARER_TOKEN=true
  export TANAGRA_AUTH_IAP_GKE_JWT=false
fi

echo

if [[ ${debugJvm} ]]; then
  # ./gradlew service:bootRun --debug-jvm
    echo "Enabling server jvm debug"
    echo "Listening for transport dt_socket at address: 5005"
  ./gradlew service:bootRun -Dagentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
else
  ./gradlew service:bootRun
fi



