SELECT
  mo.measurement_id,
  mo.person_id

FROM `${omopDataset}.measurement` AS mo

WHERE mo.measurement_source_value IN ('BMI_CLEAN', 'BMI')
