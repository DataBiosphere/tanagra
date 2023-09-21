/*
 platform_id column has type NUMERIC. This causes index column to be FLOAT.
 Currently indexing doesn't handle floats properly, so change column to INTEGER.
*/
SELECT CAST(PARSE_NUMERIC(platform_id) as INT64) AS platform_id, assay_name as assay_name
FROM `sd-vumc-tanagra-test.sd_20230331.platform`
UNION ALL
/*
 Add some rows to get hierarchy to work. Parent ids are defined in
 genotype_criteria, criteria_meta_seq column. criteria_meta_seq is not in the
 same "ID space" as platform_id. Tanagra requres parent id to be in same "ID
 space" as platform_id. So create artificial platform_ids for parents.
 */
(SELECT 101 AS platform_id, 'GWAS Platforms' AS assay_name UNION ALL
 SELECT 102, 'Non-GWAS and Targeted Genotyping Platforms' UNION ALL
 SELECT 103, 'Sequencing Platforms')
