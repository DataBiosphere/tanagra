SELECT CAST(genotype_result_id AS INT64) AS genotype_result_id, person_id, CAST(platform_id AS INT64) AS platform_id
FROM `sd-vumc-tanagra-test.sd_20230331`.x_genotype_result
