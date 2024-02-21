SELECT
  mo.measurement_id,
  mo.person_id,
  (SELECT COUNT(*) FROM `${omopDataset}.x_vs_wh` AS xvw
   WHERE xvw.measurement_id = mo.measurement_id AND xvw.x_invalid = 'N') > 1 AS is_clean,
  mo.measurement_date,
  mo.value_as_number,
  mo.value_as_concept_id,
  mvc.concept_name AS value_as_concept_name,
  mo.unit_concept_id,
  muc.concept_name AS unit_concept_name,
  CAST(FLOOR(TIMESTAMP_DIFF(mo.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  mo.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.measurement` AS mo

JOIN `${omopDataset}.person` AS p
    ON p.person_id = mo.person_id

LEFT JOIN `${omopDataset}.concept` AS mvc
    ON mvc.concept_id = mo.value_as_concept_id

LEFT JOIN `${omopDataset}.concept` AS muc
    ON muc.concept_id = mo.unit_concept_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = mo.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id

WHERE mo.measurement_type_concept_id = 44818701
    AND (mo.measurement_source_value = 'HEIGHT'
        OR mo.measurement_concept_id in (3035463, 3036277))
