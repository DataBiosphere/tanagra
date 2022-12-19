#!/bin/bash

setupEnvVars() {
  disableAuthChecks=$1 # 0=false, 1=true

  export TANAGRA_DATABASE_NAME=tanagra_db
  export TANAGRA_DB_INITIALIZE_ON_START=false
  export TANAGRA_DB_USERNAME=dbuser
  export TANAGRA_DB_PASSWORD=dbpwd

  export TANAGRA_UNDERLAY_FILES=broad/aou_synthetic/expanded/aou_synthetic.json,broad/cms_synpuf/expanded/cms_synpuf.json
  export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true
  export TANAGRA_AUTH_IAP_GKE_JWT=false

  if [ $disableAuthChecks == 1 ]; then
    export TANAGRA_AUTH_DISABLE_CHECKS=true
    export TANAGRA_AUTH_BEARER_TOKEN=false
  else
    export TANAGRA_AUTH_DISABLE_CHECKS=false
    export TANAGRA_AUTH_BEARER_TOKEN=true
  fi
}

COMMAND=$1
if [ ${#@} == 0 ]; then
    setupEnvVars 0
elif [ $COMMAND = "disable-auth" ]; then
    setupEnvVars 1
else
    echo "Usage: $0 [disable-auth]"
    exit 1
fi

./gradlew bootRun
