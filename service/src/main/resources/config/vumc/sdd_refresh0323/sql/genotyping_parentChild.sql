SELECT
/* Use parent ids defined in platform.sql */
    parent_seq + 100 AS parent,
    CAST(platform_id as INT64) AS child
FROM `sd-vumc-tanagra-test.sd_20230331.genotype_criteria` g, `sd-vumc-tanagra-test.sd_20230331.platform` p
WHERE g.name = p.assay_name
