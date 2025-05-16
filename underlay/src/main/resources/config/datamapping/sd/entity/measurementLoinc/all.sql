SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept
FROM `${omopDataset}.concept`
WHERE
    vocabulary_id = 'LOINC'
    AND concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')

UNION ALL
-- non-hierarchy LOINC concepts
SELECT -- 165
       concept_id,
       concept_name,
       vocabulary_id,
       concept_code,
       'Standard' AS standard_concept
FROM `${omopDataset}.concept` c
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'LOINC'
  AND c.standard_concept = 'S'
  AND c.concept_class_id = 'Clinical Observation'
  AND c.concept_id IN (
    SELECT DISTINCT measurement_concept_id FROM `${omopDataset}.measurement`
)

UNION ALL
-- non-hierarchy HCPCS concepts
SELECT -- 23
       concept_id,
       concept_name,
       vocabulary_id,
       concept_code,
       'Standard' AS standard_concept
FROM `${omopDataset}.concept` c
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'HCPCS'
  AND c.standard_concept = 'S'
  AND c.concept_id IN (
    SELECT DISTINCT measurement_concept_id FROM `${omopDataset}.measurement`
)
