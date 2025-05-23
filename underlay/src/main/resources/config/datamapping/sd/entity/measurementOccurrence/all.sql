SELECT
  mo.measurement_id,
  mo.person_id,
  p.person_source_value,
  mo.measurement_concept_id,
  mc.concept_name AS measurement_concept_name,
  mo.measurement_date,
  mo.value_as_number,
  mo.value_as_concept_id,
  evc.concept_name AS value_as_concept_name,
  mo.unit_concept_id,
  uc.concept_name AS unit_concept_name,
  mo.measurement_source_value,
  mo.measurement_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(mo.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  mo.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name,
  mo.measurement_type_concept_id,
  tc.concept_name AS measurement_type_concept_name
FROM `${omopDataset}.measurement` AS mo
JOIN `${omopDataset}.person` AS p
    ON p.person_id = mo.person_id
LEFT JOIN `${omopDataset}.concept` AS mc
    ON mc.concept_id = mo.measurement_concept_id
    AND mc.domain_id = 'Measurement'
LEFT JOIN `${omopDataset}.concept` AS evc
    ON evc.concept_id = mo.value_as_concept_id
LEFT JOIN `${omopDataset}.concept` AS uc
    ON uc.concept_id = mo.unit_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
ON vo.visit_occurrence_id = mo.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
LEFT JOIN `${omopDataset}.concept` AS tc
    ON tc.concept_id = mo.measurement_type_concept_id
WHERE mo.measurement_concept_id IS NOT null
  AND mo.measurement_concept_id != 0
