SELECT
  mo.measurement_id,
  mo.person_id

FROM `${omopDataset}.measurement` AS mo

WHERE mo.measurement_type_concept_id = 44818701
    AND (mo.measurement_source_value = 'HEIGHT'
        OR mo.measurement_concept_id in (3035463, 3036277))
