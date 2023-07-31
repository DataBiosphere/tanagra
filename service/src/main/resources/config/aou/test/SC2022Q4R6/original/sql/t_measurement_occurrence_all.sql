SELECT
  mo.measurement_id AS id, mo.person_id, mo.measurement_concept_id, mo.measurement_date,
  mo.value_as_number AS value_numeric, mo.value_as_concept_id, mo.unit_concept_id,
  mo.measurement_source_value AS source_value, mo.measurement_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(mo.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  mo.visit_occurrence_id, vo.visit_concept_id

FROM `all-of-us-ehr-dev.SC2022Q4R6.measurement` AS mo

JOIN `all-of-us-ehr-dev.SC2022Q4R6.person` AS p
ON p.person_id = mo.person_id

LEFT JOIN `all-of-us-ehr-dev.SC2022Q4R6.visit_occurrence` AS vo
ON vo.visit_occurrence_id = mo.visit_occurrence_id
