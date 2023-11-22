#!/usr/bin/env bash

# Generate the documentation and verify that there are no changes from what is currently in the branch.

./gradlew documentation:writeOutputFile -Pgenerator=DEPLOYMENT_CONFIG

exit_code=0
diffOutput=$(git diff --cached)
if [[ $(echo ${diff_output} | wc -c) -gt 1 ]]
  then
    printf "Differences found:\n${diff_output}\n"
    printf "Please regenerate documentation files and add to your PR.\n"
    exit_code=1
fi

exit ${exit_code}
