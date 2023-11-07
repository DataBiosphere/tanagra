SELECT
    concept_id
FROM `${omopDataset}.concept`
WHERE
    domain_id = 'Measurement'
    AND vocabulary_id = 'SNOMED'
