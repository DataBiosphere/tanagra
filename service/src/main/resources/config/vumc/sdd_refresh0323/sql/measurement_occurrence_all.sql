SELECT
  mo.measurement_id AS id, mo.person_id,
  (CASE WHEN mo.measurement_concept_id IS NULL THEN 0 ELSE mo.measurement_concept_id END) AS measurement_concept_id,
  mo.measurement_date,
  mo.value_as_number AS value_numeric, mo.value_as_concept_id, mo.unit_concept_id,
  mo.measurement_source_value AS source_value, mo.measurement_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(mo.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  mo.visit_occurrence_id, vo.visit_concept_id

FROM `sd-vumc-tanagra-test.sd_20230331.measurement` AS mo

JOIN `sd-vumc-tanagra-test.sd_20230331.person` AS p
ON p.person_id = mo.person_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.visit_occurrence` AS vo
ON vo.visit_occurrence_id = mo.visit_occurrence_id
