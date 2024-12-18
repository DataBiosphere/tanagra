SELECT
    DISTINCT c.concept_id,
             c.concept_name,
             c.vocabulary_id,
             v.vocabulary_name,
             c.concept_code,
             'Standard' AS standard_concept
FROM `${omopDataset}.observation` AS o
JOIN `${omopDataset}.concept` AS c ON c.concept_id = o.observation_concept_id
WHERE c.domain_id = 'Observation'
  AND c.standard_concept = 'S'
  AND c.vocabulary_id != 'PPI'
  AND c.concept_class_id != 'Survey'