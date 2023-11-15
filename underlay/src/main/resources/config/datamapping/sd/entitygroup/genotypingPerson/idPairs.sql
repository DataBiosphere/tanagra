SELECT
    CAST(genotype_result_id AS INT64) AS genotype_result_id,
    person_id,
    CAST(platform_id AS INT64) AS platform_id
FROM `${omopDataset}`.x_genotype_result
