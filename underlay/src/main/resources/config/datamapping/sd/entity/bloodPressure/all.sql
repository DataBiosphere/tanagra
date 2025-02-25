SELECT
  ROW_NUMBER() OVER (ORDER BY xbp.person_id, xbp.measurement_datetime) AS id,
  xbp.person_id,
  p.person_source_value,
  xbp.measurement_datetime,
  xbp.systolic,
  xbp.diastolic,
  xbp.bp,
  CAST(xbp.status_code AS INT64) AS status_code,
  CASE
    WHEN status_code = '1' THEN 'Pre Hypertensive'
    WHEN status_code = '2' THEN 'Hypertensive'
    WHEN status_code = '3' THEN 'Hypotensive'
    WHEN status_code = '4' THEN 'Normal'
    ELSE 'Unknown'
  END AS status_code_name,
  CAST(FLOOR(TIMESTAMP_DIFF(xbp.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  xbp.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.x_blood_pressure` AS xbp

JOIN `${omopDataset}.person` AS p
    ON p.person_id = xbp.person_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = xbp.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
