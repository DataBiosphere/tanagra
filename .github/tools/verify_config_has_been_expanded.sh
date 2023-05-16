#!/usr/bin/env bash

# This script will fail if original and expanded config are not in sync in a PR.

# For each underlay, this script expands config. Afterwards, if git reports any
# modified files, this script will fail.

# Expand configs for all Broad, VUMC underlays. (This repo's SA doesn't have
# permission to read Verily source datasets.)
underlay_root_dirs=$(find service/src/main/resources/config -mindepth 2 -maxdepth 2 -type d | grep -v verily)
for underlay_root_dir in ${underlay_root_dirs}
do
  echo "Expanding config for ${underlay_root_dir}"
  underlay_absolute_root_dir=$(realpath ${underlay_root_dir})
  underlay_name=$(basename ${underlay_root_dir})
  original_json_file_path=$(echo "${underlay_absolute_root_dir}/original/${underlay_name}.json")
  expanded_dir=$(echo "${underlay_absolute_root_dir}/expanded")
  ./gradlew indexer:index -Dexec.args="EXPAND_CONFIG ${original_json_file_path} ${expanded_dir}"
done

# Check for modified files
# https://stackoverflow.com/a/61748401/6447189
modified_files=$(git status -s -uno service/src/main/resources/config)

if [[ $(echo ${modified_files} | wc -c) -eq 1 ]]
then
  echo "No modified files found"
  exit
fi

git diff --cached .

echo "Modified files found:"
echo "${modified_files}"
echo "Please expand and add those files to your PR."
exit 1
