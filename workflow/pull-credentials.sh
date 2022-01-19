#!/bin/bash

## This script pulls from Vault the SA key file needed for generating index tables.
## Dependencies: vault
## Inputs: VAULT_TOKEN (arg, optional) default is $HOME/.vault-token
## Usage: ./render-config.sh

## The script assumes that it is being run from the project-level directory "tanagra/workflow/".
if [ $(basename $PWD) != 'workflow' ]; then
  echo "Script must be run from project-level directory 'tanagra/workflow/'"
  exit 1
fi

VAULT_TOKEN=${1:-$(cat $HOME/.vault-token)}
DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:consul-0.20.0
DEV_SA_VAULT_PATH=secret/dsde/terra/tanagra/dev/tanagra_sa.json

# Helper function to read a secret from Vault and write it to a local file in the rendered/ directory.
# Inputs: vault path, file name, [optional] decode from base 64
# Usage: readFromVault $CI_SA_VAULT_PATH ci-account.json
#        readFromValue $JANITOR_CLIENT_SA_VAULT_PATH janitor-client.json base64
readFromVault () {
  vaultPath=$1
  fileName=$2
  decodeBase64=$3
  if [ -z "$vaultPath" ] || [ -z "$fileName" ]; then
    echo "Two arguments required for readFromVault function"
    exit 1
  fi
  if [ -z "$decodeBase64" ]; then
    docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
              vault read -format json $vaultPath \
              | jq -r .data > "rendered/$fileName"
  else
    docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
              vault read -format json $vaultPath \
              | jq -r .data.key | base64 -d > "rendered/$fileName"
  fi
  return 0
}

mkdir -p rendered

# used for generating new index tables in the broad-tanagra-dev project
echo "Reading the dev service account key file from Vault"
readFromVault "$DEV_SA_VAULT_PATH" "tanagra_sa.json"
