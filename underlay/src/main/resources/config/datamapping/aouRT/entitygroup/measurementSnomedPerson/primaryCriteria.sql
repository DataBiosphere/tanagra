SELECT
  mo.person_id,
  (CASE WHEN mo.measurement_concept_id IS NULL THEN 0 ELSE mo.measurement_concept_id END) AS measurement_concept_id

FROM `${omopDataset}.measurement` AS mo
