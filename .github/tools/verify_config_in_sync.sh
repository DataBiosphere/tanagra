#!/usr/bin/env bash

# For example, verify that vumc/sdd is the same as verily/sdd.
# The only difference is sdd.json. So verify that entity/, entitygroup/, sql/
# and ui/ are the same.


configs_to_compare_list="
broad/cms_synpuf verily/cms_synpuf\n
broad/aou_synthetic verily/aou_synthetic\n
vumc/sdd verily/sdd
"

# Needed for for loop to split only on newline, not on space
IFS='
'

exit_code=0
for configs_to_compare in $(echo -e ${configs_to_compare_list})
do
  underlay_1=$(echo ${configs_to_compare} | awk '{print $1}')
  underlay_2=$(echo ${configs_to_compare} | awk '{print $2}')
  echo "Comparing ${underlay_1} to ${underlay_2}"

  underlay_dir_1=$(echo service/src/main/resources/config/${underlay_1})
  underlay_dir_2=$(echo service/src/main/resources/config/${underlay_2})
  diff_output=$(diff -rq ${underlay_dir_1} ${underlay_dir_2})

  if [[ $(echo ${diff_output} | wc -c) -gt 1 ]]
  then
    echo "${underlay_1} and ${underlay_2} are different:"
    echo "${diff_output}"
    printf "Please update and add files to your PR.\n\n"
    exit_code=1
  fi
done

exit ${exit_code}
