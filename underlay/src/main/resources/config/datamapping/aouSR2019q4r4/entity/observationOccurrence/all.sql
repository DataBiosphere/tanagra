SELECT
  o.observation_id,
  o.person_id,
  o.observation_concept_id,
  oc.concept_name AS observation_concept_name,
  o.observation_datetime,
  o.value_as_string,
  o.value_as_concept_id,
  ovc.concept_name AS value_as_concept_name,
  o.unit_concept_id,
  ouc.concept_name AS unit_concept_name,
  o.observation_source_value,
  o.observation_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(o.observation_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  o.visit_occurrence_id

FROM `${omopDataset}.observation` AS o

JOIN `${omopDataset}.person` AS p
    ON p.person_id = o.person_id

JOIN `${omopDataset}.concept` AS oc
    ON oc.concept_id = o.observation_concept_id

LEFT JOIN `${omopDataset}.concept` AS ovc
    ON ovc.concept_id = o.value_as_concept_id

LEFT JOIN `${omopDataset}.concept` AS ouc
    ON ouc.concept_id = o.unit_concept_id
