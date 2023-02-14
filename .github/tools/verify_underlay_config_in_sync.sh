#!/usr/bin/env bash

# Verify that vumc/sdd is the same as verily/sdd.
# The only difference is sdd.json. So verify that entity/, entitygroup/, sql/
# and ui/ are the same.


configs_to_compare_list="
broad/cms_synpuf verily/cms_synpuf\n
broad/aou_synthetic verily/aou_synthetic\n
vumc/sdd verily/sdd
"

# Needed for loop to split only on newline, not on space
IFS='
'

for configs_to_compare in $(echo -e ${configs_to_compare_list})
do
  underlay_1=$(echo ${configs_to_compare} | awk '{print $1}')
  underlay_2=$(echo ${configs_to_compare} | awk '{print $2}')
  echo "Comparing ${underlay_1} to ${underlay_2}"
done
