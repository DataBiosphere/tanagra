SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept
FROM `${omopDataset}.concept`
WHERE domain_id = 'Condition'
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
