SELECT
  mo.person_id,
  mo.measurement_concept_id
FROM `${omopDataset}.measurement` AS mo
WHERE mo.measurement_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept`
      WHERE vocabulary_id = 'LOINC'
        AND concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test'))
