SELECT
  de.drug_exposure_id,
  de.person_id,
  p.person_source_value,
  de.drug_concept_id,
  ic.concept_name AS drug_concept_name,
  de.drug_exposure_start_date,
  de.drug_exposure_end_date,
  de.stop_reason,
  de.refills,
  de.days_supply,
  de.drug_source_value,
  de.drug_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(de.drug_exposure_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.drug_exposure` AS de

JOIN `${omopDataset}.person` AS p
    ON p.person_id = de.person_id

JOIN `${omopDataset}.concept` AS ic
    ON ic.concept_id = de.drug_concept_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = de.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
