SELECT *
FROM `all-of-us-ehr-dev.SR2023Q3R1.concept`
WHERE vocabulary_id = 'LOINC'
  AND concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')