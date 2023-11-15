SELECT DISTINCT c.*
FROM `all-of-us-ehr-dev.SC2023Q3R1.measurement` m
JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` c ON m.measurement_concept_id = c.concept_id
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'HCPCS'
  AND c.standard_concept = 'S'
UNION DISTINCT
SELECT DISTINCT c.*
FROM `all-of-us-ehr-dev.SC2023Q3R1.measurement` m
JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` c ON m.measurement_concept_id = c.concept_id
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'LOINC'
  AND c.standard_concept = 'S'
  AND c.concept_class_id = 'Clinical Observation'