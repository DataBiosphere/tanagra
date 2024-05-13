SELECT
  o.observation_id,
  o.person_id,
  o.observation_datetime,
  o.value_as_number,
  ps.id AS survey_item_id,

  ps_survey.concept_id AS survey_concept_id,
  sc.concept_name AS survey_concept_name,
  o.observation_source_concept_id AS question_concept_id,
  qc.concept_name AS question_concept_name,
  o.value_source_concept_id AS answer_concept_id,
  ac.concept_name AS answer_concept_name

FROM `${omopDataset}.observation` AS o

JOIN `${omopDataset}.prep_survey` AS ps
    ON ps.concept_id = o.observation_source_concept_id
    AND (
      (CAST(ps.value AS INT64) = o.value_source_concept_id)
      OR
      (ps.value IS NULL AND o.value_source_concept_id IS NULL)
    )
    AND ps.subtype = 'ANSWER'

JOIN `${omopDataset}.prep_survey` AS ps_survey
    ON ps_survey.survey = ps.survey
    AND ps_survey.subtype = 'SURVEY'
LEFT JOIN `${omopDataset}.concept` AS sc
    ON sc.concept_id = ps_survey.concept_id
LEFT JOIN `${omopDataset}.concept` AS qc
    ON qc.concept_id = o.observation_source_concept_id
LEFT JOIN `${omopDataset}.concept` AS ac
    ON ac.concept_id = o.value_source_concept_id

WHERE ps.survey = 'Basics'
