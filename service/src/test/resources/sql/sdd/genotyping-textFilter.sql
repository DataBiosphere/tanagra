SELECT g.id AS id, g.name AS name FROM `verily-tanagra-dev.sdstatic_index_022723`.genotyping AS g WHERE CONTAINS_SUBSTR(g.id, 'Illumina') LIMIT 30
