SELECT g.id AS id, g.name AS name FROM `verily-tanagra-dev.sd20230331_index_092123`.genotyping AS g WHERE REGEXP_CONTAINS(UPPER(g.id), UPPER('Illumina')) LIMIT 30
