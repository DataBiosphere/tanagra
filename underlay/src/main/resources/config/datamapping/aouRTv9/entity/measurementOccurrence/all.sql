SELECT
  mo.measurement_id,
  mo.person_id,
  (CASE WHEN mo.measurement_concept_id IS NULL THEN 0 ELSE mo.measurement_concept_id END) AS measurement_concept_id,
  mc.concept_name AS standard_concept_name,
  mc.concept_code AS standard_concept_code,
  mc.vocabulary_id AS standard_vocabulary,
  mo.measurement_datetime,
  mo.measurement_type_concept_id,
  mt.concept_name as measurement_type_concept_name,
  mo.operator_concept_id,
  mop.concept_name as operator_concept_name,
  mo.value_as_number,
  mo.value_as_concept_id,
  evc.concept_name AS value_as_concept_name,
  mo.unit_concept_id,
  uc.concept_name AS unit_concept_name,
  mo.range_low,
  mo.range_high,
  mo.measurement_source_value,
  mo.measurement_source_concept_id,
  ms.concept_name as source_concept_name,
  ms.concept_code as source_concept_code,
  ms.vocabulary_id as source_vocabulary,
  mo.unit_source_value,
  mo.value_source_value,
  CAST(FLOOR(TIMESTAMP_DIFF(mo.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  mo.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_occurrence_concept_name
FROM `${omopDataset}.measurement` AS mo
JOIN `${omopDataset}.person` AS p ON p.person_id = mo.person_id
LEFT JOIN `${omopDataset}.concept` AS mc ON mc.concept_id = mo.measurement_concept_id
LEFT JOIN `${omopDataset}.concept` AS evc ON evc.concept_id = mo.value_as_concept_id
LEFT JOIN `${omopDataset}.concept` AS uc ON uc.concept_id = mo.unit_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = mo.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
LEFT JOIN `${omopDataset}.concept` AS mt ON mt.concept_id = mo.measurement_type_concept_id
LEFT JOIN `${omopDataset}.concept` AS mop ON mop.concept_id = mo.operator_concept_id
LEFT JOIN `${omopDataset}.concept` AS ms ON ms.concept_id = mo.measurement_source_concept_id
WHERE mo.measurement_concept_id IS NOT null
  AND mo.measurement_concept_id != 0
