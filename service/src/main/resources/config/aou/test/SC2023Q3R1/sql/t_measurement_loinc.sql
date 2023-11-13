SELECT *
FROM `all-of-us-ehr-dev.SC2023Q3R1.concept` c
WHERE c.vocabulary_id = 'LOINC'
  AND c.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')