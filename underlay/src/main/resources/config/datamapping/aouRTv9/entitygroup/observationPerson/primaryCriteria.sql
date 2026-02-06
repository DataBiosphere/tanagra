SELECT
  o.person_id,
  o.observation_concept_id
FROM `${omopDataset}.observation` AS o
WHERE o.observation_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept` oc
      WHERE oc.domain_id = 'Observation'
        AND oc.standard_concept = 'S'
        AND oc.vocabulary_id != 'PPI'
        AND oc.concept_class_id != 'Survey'
      )
