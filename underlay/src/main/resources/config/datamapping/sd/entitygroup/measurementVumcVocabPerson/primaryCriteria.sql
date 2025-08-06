SELECT
  mo.person_id,
  mo.measurement_concept_id
FROM `${omopDataset}.measurement` AS mo
WHERE mo.measurement_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Measurement'
        AND c.vocabulary_id = 'VUMC Meas Value'
      )
AND mo.measurement_concept_id IS NOT NULL
AND mo.measurement_concept_id != 0
