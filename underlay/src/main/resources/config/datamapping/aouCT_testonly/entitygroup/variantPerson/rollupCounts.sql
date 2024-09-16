SELECT v.variant_row_num, ARRAY_LENGTH(vtop.person_ids) AS num_persons
/* Wrap variant_to_person table in a SELECT DISTINCT because there is a duplicate row in the test data. */
FROM (SELECT DISTINCT vid, person_ids FROM `${omopDataset}.variant_to_person`) AS vtop
JOIN
  (SELECT ROW_NUMBER() OVER (ORDER BY vid) AS variant_row_num, vid FROM `${omopDataset}.prep_vat`)
  AS v ON v.vid = vtop.vid
