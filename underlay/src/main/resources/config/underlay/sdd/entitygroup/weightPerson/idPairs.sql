SELECT
  mo.measurement_id,
  mo.person_id

FROM `${omopDataset}.measurement` AS mo

WHERE mo.measurement_type_concept_id = 44818701
    AND (mo.measurement_source_value = 'WEIGHT'
        OR mo.measurement_concept_id in (3011043, 3023166, 3025315, 21492642))

