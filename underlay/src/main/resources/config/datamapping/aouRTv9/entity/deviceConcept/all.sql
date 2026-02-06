SELECT
    DISTINCT c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    v.vocabulary_name,
    c.concept_code,
    'Standard' AS standard_concept
FROM `${omopDataset}.device_exposure` AS de
JOIN `${omopDataset}.concept` AS c ON c.concept_id = de.device_concept_id
    AND c.domain_id = 'Device'
    AND c.standard_concept = 'S'
JOIN `${omopDataset}.vocabulary` AS v ON v.vocabulary_id = c.vocabulary_id

