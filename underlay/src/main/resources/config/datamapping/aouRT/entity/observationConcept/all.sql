SELECT
    DISTINCT c.concept_id,
             c.concept_name,
             c.vocabulary_id,
             c.concept_code,
             'Standard' AS standard_concept
FROM `${omopDataset}.observation` AS o
JOIN `${omopDataset}.concept` AS c ON c.concept_id = o.observation_concept_id
WHERE c.domain_id = 'Observation'
  AND o.observation_concept_id != 0
  AND c.standard_concept = 'S'
  AND c.vocabulary_id != 'PPI'
  AND c.concept_class_id != 'Survey'
  AND c.concept_id NOT IN (
     SELECT DISTINCT observation_concept_id
     FROM `${omopDataset}.observation`
     WHERE observation_source_concept_id IN (
        SELECT DISTINCT concept_id
        FROM `${omopDataset}.prep_survey`
     )
  )
