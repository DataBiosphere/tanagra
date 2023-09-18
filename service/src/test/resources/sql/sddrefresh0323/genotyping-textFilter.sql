SELECT g.id AS id, g.name AS name FROM `verily-tanagra-dev.sd20230328_index_091523`.genotyping AS g WHERE REGEXP_CONTAINS(UPPER(g.id), UPPER('Illumina')) LIMIT 30
