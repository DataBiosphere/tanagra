/* Hierarchy leaf nodes all live in genotype_criteria and x_platform. */
SELECT CAST(xp.platform_id AS INT64) platform_id, xp.assay_name
FROM `${omopDataset}`.x_platform AS xp

JOIN `${omopDataset}`.genotype_criteria AS gc
    ON UPPER(gc.name) = UPPER(xp.assay_name) AND gc.type = 'DNA'

UNION ALL

/* Hierarchy root nodes only live in genotype_criteria. */
SELECT (100 + gc.criteria_meta_seq) AS platform_id, gc.name AS assay_name
FROM `${omopDataset}`.genotype_criteria AS gc
WHERE gc.type = 'DNA' AND gc.parent_seq = 0
