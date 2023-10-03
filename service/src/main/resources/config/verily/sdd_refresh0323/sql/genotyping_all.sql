/* Hierarchy leaf nodes all live in genotype_criteria and x_platform. */
SELECT xp.platform_id, xp.assay_name
FROM `sd-vumc-tanagra-test.sd_20230331`.x_platform AS xp

JOIN `sd-vumc-tanagra-test.sd_20230331`.genotype_criteria AS gc
    ON gc.name = xp.assay_name AND gc.type = 'DNA'

UNION ALL

/* Hierarchy root nodes only live in genotype_criteria. */
SELECT (100 + gc.criteria_meta_seq) AS platform_id, gc.name AS assay_name
FROM `sd-vumc-tanagra-test.sd_20230331`.genotype_criteria AS gc
WHERE gc.type = 'DNA' AND gc.parent_seq = 0
