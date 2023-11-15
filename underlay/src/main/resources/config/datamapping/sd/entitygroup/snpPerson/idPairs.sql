SELECT 
    crit.row_num, 
    occ.snp_id, 
    occ.person_id

FROM `${omopDataset}.x_snp` AS occ

JOIN (
  SELECT crit_noid.snp_id, ROW_NUMBER() OVER (ORDER BY crit_noid.snp_id) AS row_num
  FROM (
    SELECT DISTINCT snp_id
    FROM `${omopDataset}.x_snp`
  ) AS crit_noid
) AS crit
ON crit.snp_id = occ.snp_id
