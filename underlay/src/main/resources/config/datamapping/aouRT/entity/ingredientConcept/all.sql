SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    'Standard' AS standard_concept
FROM `${omopDataset}.concept`
WHERE
    (vocabulary_id = 'ATC' AND standard_concept = 'C')
    OR (vocabulary_id IN ('RxNorm', 'RxNorm Extension') AND standard_concept = 'S')
