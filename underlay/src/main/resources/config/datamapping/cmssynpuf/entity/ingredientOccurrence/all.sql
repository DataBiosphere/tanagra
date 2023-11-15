SELECT
  de.drug_exposure_id,
  de.person_id,
  de.drug_concept_id,
  ic.concept_name AS drug_concept_name,
  de.drug_exposure_start_date,
  de.drug_exposure_end_date,
  de.stop_reason,
  de.refills,
  de.days_supply,
  de.drug_source_value,
  de.drug_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(TIMESTAMP(de.drug_exposure_start_date), TIMESTAMP(CONCAT(p.year_of_birth,'-',p.month_of_birth,'-',p.day_of_birth)), DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id

FROM `${omopDataset}.drug_exposure` AS de

JOIN `${omopDataset}.person` AS p
    ON p.person_id = de.person_id

JOIN `${omopDataset}.concept` AS ic
    ON ic.concept_id = de.drug_concept_id
