#!/usr/bin/env bash

# Generate the documentation and Typescript and verify that there are no changes from what is currently in the branch.
./gradlew annotationProcessor:run -Pgenerator=DEPLOYMENT_CONFIG_DOCS
./gradlew annotationProcessor:run -Pgenerator=UNDERLAY_CONFIG_DOCS
./gradlew annotationProcessor:run -Pgenerator=UNDERLAY_CONFIG_TYPESCRIPT

exit_code=0
diff_output=$(git diff docs/generated)
if [[ $(echo ${diff_output} | wc -c) -gt 1 ]]
  then
    printf "Differences found:\n${diff_output}\n"
    printf "Please regenerate documentation and Typescript files and add to your PR.\n"
    printf "./gradlew annotationProcessor:run -Pgenerator=DEPLOYMENT_CONFIG_DOCS\n"
    printf "./gradlew annotationProcessor:run -Pgenerator=UNDERLAY_CONFIG_DOCS\n"
    printf "./gradlew annotationProcessor:run -Pgenerator=UNDERLAY_CONFIG_TYPESCRIPT\n"
    exit_code=1
  else
    printf "No differences found\n"
fi

exit ${exit_code}
