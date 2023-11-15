SELECT
    crit_noid.snp_id,
    ROW_NUMBER() OVER (ORDER BY crit_noid.snp_id) AS row_num
FROM (
    SELECT DISTINCT snp_id
    FROM `${omopDataset}.x_snp`
) AS crit_noid
