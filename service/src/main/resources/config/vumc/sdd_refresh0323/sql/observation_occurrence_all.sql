SELECT
  o.observation_id AS id, o.person_id, o.observation_concept_id,
  o.observation_date, o.value_as_string, o.value_as_concept_id, o.unit_concept_id,
  o.observation_source_value AS source_value, o.observation_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(o.observation_date, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  o.visit_occurrence_id, vo.visit_concept_id

FROM `sd-vumc-tanagra-test.sd_20230328.observation` AS o

JOIN `sd-vumc-tanagra-test.sd_20230328.person` AS p
ON p.person_id = o.person_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230328.visit_occurrence` AS vo
ON vo.visit_occurrence_id = o.visit_occurrence_id
