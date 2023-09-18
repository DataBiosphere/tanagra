SELECT s.id AS id, s.name AS name FROM `verily-tanagra-dev.sd20230328_index_091523`.snp AS s WHERE REGEXP_CONTAINS(UPPER(s.id), UPPER('RS1292')) LIMIT 30
