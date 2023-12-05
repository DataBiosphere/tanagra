#!/bin/bash

## This script sets up the environment for local development.
## Dependencies: chmod
## Usage: source tools/local-dev.sh

function check_java_version() {
  local REQ_JAVA_VERSION=11

  echo "--  Checking if installed Java version is ${REQ_JAVA_VERSION} or higher"
  if [[ -n "$(which java)" ]]; then
    # Get the current major version of Java: "11.0.12" => "11"
    local CUR_JAVA_VERSION="$(java -version 2>&1 | awk -F\" '{ split($2,a,"."); print a[1]}')"
    if [[ "${CUR_JAVA_VERSION}" -lt ${REQ_JAVA_VERSION} ]]; then
      >&2 echo "ERROR: Java version detected (${CUR_JAVA_VERSION}) is less than required (${REQ_JAVA_VERSION})"
      return 1
    fi
  else
    >&2 echo "ERROR: No Java installation detected"
    return 1
  fi
}

if ! check_java_version; then
  unset check_java_version
  return 1
fi
unset check_java_version

## The script assumes that it is being run from the top-level directory "tanagra/".
if [[ "$(basename "$PWD")" != 'tanagra' ]]; then
  >&2 echo "ERROR: Script must be run from top-level directory 'tanagra/'"
  return 1
fi

echo "Building Java code"
./gradlew clean indexer:install

echo "Aliasing JAR file"
alias tanagra="$(pwd)"/indexer/build/install/tanagra/bin/tanagra

echo "Making all 'tools' scripts executable"
chmod a+x ./indexer/tools/*
