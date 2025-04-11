SELECT DISTINCT
  o.observation_id,
  o.person_id,
  p.person_source_value,
  o.observation_concept_id,
  oc.concept_name AS observation_concept_name,
  oc.concept_code AS observation_concept_code,
  o.observation_date,
  o.value_as_string,
  o.value_as_concept_id,
  ovc.concept_name AS value_as_concept_name,
  o.unit_concept_id,
  ouc.concept_name AS unit_concept_name,
  (CASE WHEN oc.standard_concept IS NULL THEN 'Source' WHEN oc.standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept,
  o.observation_source_value,
  o.observation_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(o.observation_date, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  o.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name
FROM `${omopDataset}.observation` AS o
JOIN `${omopDataset}.person` AS p
    ON p.person_id = o.person_id
JOIN `${omopDataset}.concept` AS oc
    ON oc.concept_id = o.observation_concept_id
    AND oc.domain_id = 'Observation'
    AND oc.standard_concept = 'S'
    AND oc.vocabulary_id != 'PPI'
    AND oc.concept_class_id != 'Survey'
LEFT JOIN `${omopDataset}.concept` AS ovc
    ON ovc.concept_id = o.value_as_concept_id
LEFT JOIN `${omopDataset}.concept` AS ouc
    ON ouc.concept_id = o.unit_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = o.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
WHERE o.observation_concept_id IS NOT null
  AND o.observation_concept_id != 0
