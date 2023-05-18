SELECT
/* Use parent ids defined in platform.sql */
    parent_seq + 100 AS parent,
    CAST(platform_id as INT64) AS child
FROM `victr-tanagra-test.sd_20230328.genotype_criteria` g, `victr-tanagra-test.sd_20230328.platform` p
WHERE g.name = p.assay_name
