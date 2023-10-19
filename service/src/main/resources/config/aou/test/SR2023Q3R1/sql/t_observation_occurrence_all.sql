SELECT
  o.observation_id AS id, o.person_id, o.observation_concept_id,
  o.observation_date, o.value_as_string, o.value_as_concept_id, o.unit_concept_id,
  o.observation_source_value AS source_value, o.observation_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(o.observation_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  o.visit_occurrence_id, vo.visit_concept_id

FROM `all-of-us-ehr-dev.SR2023Q3R1.observation` AS o

JOIN `all-of-us-ehr-dev.SR2023Q3R1.person` AS p
ON p.person_id = o.person_id

LEFT JOIN `all-of-us-ehr-dev.SR2023Q3R1.visit_occurrence` AS vo
ON vo.visit_occurrence_id = o.visit_occurrence_id

LEFT JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` AS c
          ON vo.visit_concept_id = c.concept_id

WHERE c.domain_id = 'Visit'
  AND c.standard_concept = 'S'
