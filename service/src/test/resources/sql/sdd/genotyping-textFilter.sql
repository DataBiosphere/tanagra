SELECT g.id AS id, g.name AS name FROM `verily-tanagra-dev.sdstatic_index_011923`.genotyping AS g WHERE CONTAINS_SUBSTR(g.id, 'Illumina') LIMIT 30
