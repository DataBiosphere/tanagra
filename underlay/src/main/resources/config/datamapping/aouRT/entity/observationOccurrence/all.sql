SELECT
  o.observation_id,
  o.person_id,
  o.observation_concept_id,
  oc.concept_name AS standard_concept_name,
  oc.concept_code AS standard_concept_code,
  oc.vocabulary_id AS standard_vocabulary,
  o.observation_date,
  o.observation_datetime,
  o.observation_type_concept_id,
  ot.concept_name as observation_type_concept_name,
  o.value_as_number,
  o.value_as_string,
  o.value_as_concept_id,
  ovc.concept_name AS value_as_concept_name,
  o.qualifier_concept_id,
  oq.concept_name as qualifier_concept_name,
  o.unit_concept_id,
  o.unit_source_value,
  o.qualifier_source_value,
  o.value_source_concept_id,
  o.value_source_value,
  o.questionnaire_response_id,
  ouc.concept_name AS unit_concept_name,
  o.observation_source_value,
  o.observation_source_concept_id,
  os.concept_name as source_concept_name,
  os.concept_code as source_concept_code,
  os.vocabulary_id as source_vocabulary,
  CAST(FLOOR(TIMESTAMP_DIFF(o.observation_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  o.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_occurrence_concept_name
FROM `${omopDataset}.observation` AS o
JOIN `${omopDataset}.person` AS p ON p.person_id = o.person_id
JOIN `${omopDataset}.concept` AS oc ON oc.concept_id = o.observation_concept_id
    AND oc.domain_id = 'Observation'
    AND oc.standard_concept = 'S'
    AND oc.vocabulary_id != 'PPI'
    AND oc.concept_class_id != 'Survey'
LEFT JOIN `${omopDataset}.concept` AS ovc ON ovc.concept_id = o.value_as_concept_id
LEFT JOIN `${omopDataset}.concept` AS ouc ON ouc.concept_id = o.unit_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = o.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
LEFT JOIN `${omopDataset}.concept` AS ot ON ot.concept_id = o.observation_type_concept_id
LEFT JOIN `${omopDataset}.concept` AS os ON os.concept_id = o.observation_source_concept_id
LEFT JOIN `${omopDataset}.concept` AS oq ON oq.concept_id = o.qualifier_concept_id
