SELECT
  de.device_exposure_id AS id, de.person_id, de.device_concept_id,
  de.device_exposure_start_date AS start_date, de.device_exposure_end_date AS end_date,
  de.device_source_value AS source_value, de.device_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(de.device_exposure_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id, vo.visit_concept_id

FROM `sd-vumc-tanagra-test.sd_20230328.device_exposure` AS de

JOIN `sd-vumc-tanagra-test.sd_20230328.person` AS p
ON p.person_id = de.person_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230328.visit_occurrence` AS vo
ON vo.visit_occurrence_id = de.visit_occurrence_id
