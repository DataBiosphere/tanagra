#!/usr/bin/env bash

first_release=$1
last_release=$2
output_file="diffReleases.txt"

if [ -z $first_release ] || [ -z $last_release ];
then
  echo "First and last release tags are required (e.g. 0.0.370).";
  exit 1;
fi
echo "First release tag: $first_release"
echo "Last release tag: $last_release"

diff_output=$(git diff "$first_release..$last_release" -- underlay/ indexer/)
if [ -z $diff_output ];
then
  echo "REINDEXING NOT NEEDED" > $output_file
else
  echo "REINDEXING RECOMMENDED" > $output_file
fi
echo >> $output_file

git log "$first_release..$last_release" --grep='^bump' --invert-grep --pretty=format:"%h %as %an%x09%s" >> $output_file

cat $output_file
