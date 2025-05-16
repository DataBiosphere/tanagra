SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    'Standard' AS standard_concept
FROM `${omopDataset}.concept`
WHERE domain_id = 'Condition'
  AND vocabulary_id = 'SNOMED'
  AND standard_concept = 'S'

UNION DISTINCT
-- non-hierarchy concepts
SELECT DISTINCT
                concept_id,
                concept_name,
                vocabulary_id,
                concept_code,
                'Standard' AS standard_concept
FROM `${omopDataset}.concept` c
WHERE c.concept_id in (
    SELECT DISTINCT condition_concept_id from `${omopDataset}.condition_occurrence`
    )
    AND c.vocabulary_id != 'SNOMED'
    AND c.standard_concept = 'S'
