SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept
FROM `${omopDataset}.concept`
WHERE domain_id = 'Observation'
    AND standard_concept = 'S'
    AND vocabulary_id != 'PPI'
    AND concept_class_id != 'Survey'
