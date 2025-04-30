SELECT
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    c.concept_code,
    'Standard' AS standard_concept
FROM (SELECT DISTINCT measurement_concept_id FROM `${omopDataset}.measurement`) m
JOIN `${omopDataset}.concept` c ON m.measurement_concept_id = c.concept_id
WHERE c.domain_id = 'Measurement'
  AND (c.vocabulary_id = 'HCPCS'
           OR (c.vocabulary_id = 'LOINC' AND c.concept_class_id = 'Clinical Observation')
      )
  AND c.standard_concept = 'S'
