SELECT
  mo.measurement_id,
  mo.person_id

FROM `${omopDataset}.measurement` AS mo

WHERE mo.measurement_source_value = 'Pulse' AND mo.measurement_type_concept_id = 44818701


