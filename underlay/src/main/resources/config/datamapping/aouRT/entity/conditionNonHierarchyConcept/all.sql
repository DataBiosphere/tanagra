SELECT DISTINCT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    'Standard' AS standard_concept
FROM `${omopDataset}.condition_occurrence` co
JOIN `${omopDataset}.concept` c on co.condition_concept_id = c.concept_id
    AND c.vocabulary_id != 'SNOMED'
    AND c.standard_concept = 'S'
