SELECT
  mo.person_id,
  mo.measurement_concept_id
FROM `${omopDataset}.measurement` AS mo
WHERE mo.measurement_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE domain_id = 'Measurement'
        AND vocabulary_id = 'SNOMED'
        AND standard_concept = 'S')
