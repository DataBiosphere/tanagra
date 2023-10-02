SELECT crit_noid.snp_id, ROW_NUMBER() OVER (ORDER BY crit_noid.snp_id) AS row_num
FROM (
SELECT DISTINCT snp_id
FROM `sd-vumc-tanagra-test.sd_20230331.x_snp`
) AS crit_noid
