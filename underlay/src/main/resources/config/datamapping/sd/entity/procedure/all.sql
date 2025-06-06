SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    'Standard' AS standard_concept
FROM `${omopDataset}.concept`
WHERE domain_id = 'Procedure'
  AND standard_concept = 'S'
  AND vocabulary_id = 'SNOMED'
