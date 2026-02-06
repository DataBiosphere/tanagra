SELECT
  po.person_id,
  ps.id AS survey_item_id

FROM `${omopDataset}.prep_pfhh_observation` AS po
JOIN `${omopDataset}.prep_survey` AS ps
    ON ps.concept_id = po.observation_source_concept_id
    AND (
      (CAST(ps.value AS INT64) = po.value_source_concept_id)
      OR
      (ps.value IS NULL AND po.value_source_concept_id IS NULL)
    )
    AND ps.subtype = 'ANSWER'
