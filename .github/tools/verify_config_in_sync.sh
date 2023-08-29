#!/usr/bin/env bash

# For example, verify that vumc/sdd is the same as verily/sdd.
# The only difference is sdd.json. So verify that entity/, entitygroup/, sql/
# and ui/ are the same.


configs_to_compare_list="
cms_synpuf broad/cms_synpuf verily/cms_synpuf\n
sdd_refresh0323 vumc/sdd_refresh0323 verily/sdd_refresh0323
"

# Needed for for loop to split only on newline, not on space
IFS='
'

exit_code=0
for configs_to_compare in $(echo -e ${configs_to_compare_list})
do
  underlay_name=$(echo ${configs_to_compare} | awk '{print $1}')
  underlay_1=$(echo ${configs_to_compare} | awk '{print $2}')
  underlay_2=$(echo ${configs_to_compare} | awk '{print $3}')
  printf "\nComparing ${underlay_1} to ${underlay_2}\n"

  underlay_dir_1=$(echo service/src/main/resources/config/${underlay_1})
  underlay_dir_2=$(echo service/src/main/resources/config/${underlay_2})
  # --ignore-all-space because files sometimes have newline and at of file, and sometimes don't
  diff_output=$(diff -rq --ignore-all-space --exclude ${underlay_name}.json --exclude sql ${underlay_dir_1} ${underlay_dir_2})

  if [[ $(echo ${diff_output} | wc -c) -gt 1 ]]
  then
    printf "Differences found:\n${diff_output}\n"
    printf "To see differences, run:\ndiff -r --exclude ${underlay_name}.json --exclude sql ${underlay_dir_1} ${underlay_dir_2}\n"
    printf "Please update files and add to your PR.\n"
    exit_code=1
  fi

  # Ignore legitimate differences in SQL files
  # --ignore-all-space because files sometimes have newline and at of file, and sometimes don't
  sql_diff_output=$(diff -rq --ignore-all-space --ignore-matching-lines '[FROM|JOIN|dataflowServiceAccountEmail]' ${underlay_dir_1}/sql ${underlay_dir_2}/sql)

  if [[ $(echo ${sql_diff_output} | wc -c) -gt 1 ]]
  then
    printf "Differences found:\n${sql_diff_output}\n"
    printf "To see differences, run:\ndiff -rq --ignore-all-space --ignore-matching-lines '[FROM|JOIN|dataflowServiceAccountEmail]' ${underlay_dir_1}/sql ${underlay_dir_2}/sql\n"
    printf "Please update files and add to your PR.\n"
    exit_code=1
  fi
done

exit ${exit_code}
