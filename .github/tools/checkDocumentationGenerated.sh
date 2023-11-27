#!/usr/bin/env bash

# Generate the documentation and verify that there are no changes from what is currently in the branch.

./gradlew annotationProcessor:writeOutputFile -Pgenerator=DEPLOYMENT_CONFIG_DOCS

exit_code=0
diff_output=$(git diff docs/generated)
if [[ $(echo ${diff_output} | wc -c) -gt 1 ]]
  then
    printf "Differences found:\n${diff_output}\n"
    printf "Please regenerate documentation files and add to your PR.\n"
    printf "./gradlew annotationProcessor:writeOutputFile -Pgenerator=DEPLOYMENT_CONFIG_DOCS\n"
    exit_code=1
fi

exit ${exit_code}
