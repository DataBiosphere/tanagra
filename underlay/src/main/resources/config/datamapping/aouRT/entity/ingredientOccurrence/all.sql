SELECT
  de.drug_exposure_id,
  de.person_id,
  de.drug_concept_id,
  ic.concept_name AS standard_concept_name,
  ic.concept_code AS standard_concept_code,
  ic.vocabulary_id AS standard_vocabulary,
  ic.concept_name AS drug_concept_name,
  de.drug_exposure_start_datetime,
  de.drug_exposure_end_datetime,
  de.verbatim_end_date,
  de.drug_type_concept_id,
  dt.concept_name as drug_type_concept_name,
  de.stop_reason,
  de.refills,
  de.quantity,
  de.days_supply,
  de.sig,
  de.route_concept_id,
  dr.concept_name as route_concept_name,
  de.lot_number,
  de.drug_source_value,
  de.drug_source_concept_id,
  ds.concept_name as source_concept_name,
  ds.concept_code as source_concept_code,
  ds.vocabulary_id as source_vocabulary,
  de.route_source_value,
  de.dose_unit_source_value,
  CAST(FLOOR(TIMESTAMP_DIFF(de.drug_exposure_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_occurrence_concept_name
FROM `${omopDataset}.drug_exposure` AS de
JOIN `${omopDataset}.person` AS p ON p.person_id = de.person_id
JOIN `${omopDataset}.concept` AS ic
    ON ic.concept_id = de.drug_concept_id
        AND de.drug_concept_id IS NOT null
        AND de.drug_concept_id != 0
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = de.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
LEFT JOIN `${omopDataset}.concept` AS dt ON dt.concept_id = de.drug_type_concept_id
LEFT JOIN `${omopDataset}.concept` AS dr ON dr.concept_id = de.route_concept_id
LEFT JOIN `${omopDataset}.concept` AS ds ON ds.concept_id = de.drug_source_concept_id

