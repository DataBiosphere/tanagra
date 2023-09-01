SELECT g.id AS id, g.name AS name FROM `verily-tanagra-dev.sd20230328_index_083023`.genotyping AS g WHERE CONTAINS_SUBSTR(g.id, 'Illumina') LIMIT 30
