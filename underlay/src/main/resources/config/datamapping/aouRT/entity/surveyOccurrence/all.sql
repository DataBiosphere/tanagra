SELECT
  o.observation_id,
  o.person_id,
  o.observation_datetime as survey_datetime,
  o.value_as_number,
  ps.id AS survey_item_id,

  ps_survey.concept_id AS survey_concept_id,
  sc.concept_name AS survey_concept_name,
  o.observation_source_concept_id AS question_concept_id,
  qc.name AS question_concept_name,
  o.value_source_concept_id AS answer_concept_id,
  ac.name AS answer_concept_name,
  sv.survey_concept_id as survey_version_concept_id,
  svc.concept_name as survey_version_name

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
LEFT JOIN `${omopDataset}.prep_survey` AS qc
    ON qc.concept_id = o.observation_source_concept_id 
    AND qc.subtype = 'QUESTION'
LEFT JOIN `${omopDataset}.prep_survey` AS ac
    ON ac.concept_id = o.observation_source_concept_id 
    AND CAST(ac.value AS INT64) = o.value_source_concept_id 
    AND ac.subtype = 'ANSWER'
LEFT JOIN `${omopDataset}.survey_conduct` AS sv
    ON sv.survey_conduct_id = o.questionnaire_response_id
LEFT JOIN `${omopDataset}.concept` AS svc
    ON svc.concept_id = sv.survey_concept_id

WHERE ps.survey = 'Basics'
