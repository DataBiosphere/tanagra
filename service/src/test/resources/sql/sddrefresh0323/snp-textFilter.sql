SELECT s.id AS id, s.name AS name FROM `verily-tanagra-dev.sd20230331_index_101623`.snp AS s WHERE REGEXP_CONTAINS(UPPER(s.id), UPPER('RS1292')) LIMIT 30
