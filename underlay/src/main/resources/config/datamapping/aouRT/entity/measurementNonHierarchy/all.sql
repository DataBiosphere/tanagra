SELECT DISTINCT c.*
FROM `${omopDataset}.measurement` m
JOIN `${omopDataset}.concept` c ON m.measurement_concept_id = c.concept_id
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'HCPCS'
  AND c.standard_concept = 'S'
  AND m.measurement_concept_id IS NOT null
  AND m.measurement_concept_id != 0
UNION DISTINCT
SELECT DISTINCT c.*
FROM `${omopDataset}.measurement` m
JOIN `${omopDataset}.concept` c ON m.measurement_concept_id = c.concept_id
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'LOINC'
  AND c.standard_concept = 'S'
  AND c.concept_class_id = 'Clinical Observation'
  AND m.measurement_concept_id IS NOT null
  AND m.measurement_concept_id != 0
