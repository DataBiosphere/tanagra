SELECT
  de.drug_exposure_id AS id, de.person_id, de.drug_concept_id,
  de.drug_exposure_start_date AS start_date, de.drug_exposure_end_date AS end_date,
  de.stop_reason, de.refills, de.days_supply,
  de.drug_source_value AS source_value, de.drug_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(de.drug_exposure_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id, vo.visit_concept_id

FROM `victr-tanagra-test.sd_20230328.drug_exposure` AS de

JOIN `victr-tanagra-test.sd_20230328.person` AS p
ON p.person_id = de.person_id

LEFT JOIN `victr-tanagra-test.sd_20230328.visit_occurrence` AS vo
ON vo.visit_occurrence_id = de.visit_occurrence_id
