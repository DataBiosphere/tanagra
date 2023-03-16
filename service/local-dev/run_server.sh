#!/usr/bin/env bash

usage() { echo "$0 usage flags:" && grep " .)\ #" $0; }

usage
echo

while getopts ":av" arg; do
  case $arg in
    a) # Disable authentication.
      disableAuthChecks=1
      ;;
    v) # Use Verily underlays.
      useVerilyUnderlays=1
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

if [[ ${useVerilyUnderlays} ]]; then
  echo "Using Verily underlays."
  export TANAGRA_UNDERLAY_FILES=verily/aou_synthetic/expanded/aou_synthetic.json,verily/cms_synpuf/expanded/cms_synpuf.json,verily/sdd/expanded/sdd.json
else
  echo "Using Broad underlays."
  export TANAGRA_UNDERLAY_FILES=broad/aou_synthetic/expanded/aou_synthetic.json,broad/cms_synpuf/expanded/cms_synpuf.json
fi

export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true
export TANAGRA_AUTH_IAP_GKE_JWT=false

if [[ ${disableAuthChecks} ]]; then
  echo "Disabling auth checks."
  export TANAGRA_AUTH_DISABLE_CHECKS=true
  export TANAGRA_AUTH_BEARER_TOKEN=false
else
  echo "Enabling auth checks."
  export TANAGRA_AUTH_DISABLE_CHECKS=false
  export TANAGRA_AUTH_BEARER_TOKEN=true
fi

echo

./gradlew service:bootRun
