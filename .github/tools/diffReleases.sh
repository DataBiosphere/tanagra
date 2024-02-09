#!/usr/bin/env bash

firstRelease=$1
lastRelease=$2

if [ -z $firstRelease ] || [ -z $lastRelease ];
then
  echo "First and last release tags are required (e.g. 0.0.370).";
  exit 1;
fi

git log $1..$2 --grep='^bump' --invert-grep --pretty=format:"%h %as %an%x09%s" > diffReleases.txt
