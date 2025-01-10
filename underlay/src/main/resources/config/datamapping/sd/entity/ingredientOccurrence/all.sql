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
  de.drug_type_concept_id,
  tc.concept_name AS drug_type_concept_name,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name,
  de.route_source_value AS route,
  x.x_drug_form AS drug_form,
  x.x_strength AS drug_strength,
  x.x_dose AS dose_amt,
  x.x_frequency AS drug_freq,
  x.x_doc_type AS source_table

FROM `${omopDataset}.drug_exposure` AS de

JOIN `${omopDataset}.person` AS p
    ON p.person_id = de.person_id

JOIN `${omopDataset}.x_drug_exposure` AS x
    ON de.drug_exposure_id = x.drug_exposure_id

JOIN `${omopDataset}.concept` AS ic
    ON ic.concept_id = de.drug_concept_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = de.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id

LEFT JOIN `${omopDataset}.concept` AS tc
    ON tc.concept_id = de.drug_type_concept_id
