#!/usr/bin/env bash

first_release=$1
last_release=$2

if [ -z $first_release ] || [ -z $last_release ];
then
  echo "First and last release tags are required (e.g. 0.0.370).";
  exit 1;
fi

diff_output=$(git diff $first_release..$last_release -- underlay/ indexer/)
if [ -z $diff_output ];
then
  echo "REINDEXING NOT NEEDED" > diffReleases.txt
else
  echo "REINDEXING RECOMMENDED" > diffReleases.txt
fi
echo "diff output done"

echo >> diffReleases.txt
git log $first_release..$last_release --grep='^bump' --invert-grep --pretty=format:"%h %as %an%x09%s" >> diffReleases.txt
