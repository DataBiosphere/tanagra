/*
 platform_id column has type NUMERIC. This causes index column to be FLOAT.
 Currently indexing doesn't handle floats properly, so change column to INTEGER.
*/
SELECT genotype_result_id AS genotype_result_id, person_id, CAST(platform_id as INT64) as platform_id
FROM `sd-vumc-tanagra-test.sd_20230331.x_genotype_result`
