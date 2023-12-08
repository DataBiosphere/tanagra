SELECT
  mo.person_id,
  mo.measurement_concept_id
FROM `${omopDataset}.measurement` AS mo
WHERE mo.measurement_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Measurement'
        AND c.vocabulary_id = 'HCPCS'
        AND c.standard_concept = 'S'
      UNION DISTINCT
      SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Measurement'
        AND c.vocabulary_id = 'LOINC'
        AND c.standard_concept = 'S'
        AND c.concept_class_id = 'Clinical Observation')
