SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept

FROM `${omopDataset}.concept`

WHERE
    domain_id = 'Measurement'
    AND vocabulary_id = 'SNOMED'
