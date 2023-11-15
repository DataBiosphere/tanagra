SELECT
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    v.vocabulary_name,
    c.concept_code,
    (CASE WHEN c.standard_concept IS NULL THEN 'Source' WHEN c.standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept

FROM `${omopDataset}.device_exposure` AS de

JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = de.device_concept_id

JOIN `${omopDataset}.vocabulary` AS v
    ON v.vocabulary_id = c.vocabulary_id

WHERE c.domain_id = 'Device'
    AND c.standard_concept = 'S'
    AND de.device_concept_id != 0
