SELECT v.variant_row_num, flattened_person_id
FROM `${omopDataset}.variant_to_person` AS vtop
JOIN
  (SELECT ROW_NUMBER() OVER (ORDER BY vid) AS variant_row_num, vid FROM `${omopDataset}.prep_vat`)
  AS v ON v.vid = vtop.vid
CROSS JOIN UNNEST(vtop.person_ids) AS flattened_person_id
