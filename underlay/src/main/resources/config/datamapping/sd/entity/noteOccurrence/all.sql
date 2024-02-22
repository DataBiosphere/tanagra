SELECT
  n.note_id,
  n.person_id,
  p.person_source_value,
  n.note_type_concept_id,
  nc.concept_name AS note_type_concept_name,
  n.note_date,
  n.note_title,
  n.note_text,
  n.note_source_value,
  CAST(FLOOR(TIMESTAMP_DIFF(n.note_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  n.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.note` AS n

JOIN `${omopDataset}.person` AS p
    ON p.person_id = n.person_id

JOIN `${omopDataset}.concept` AS nc
    ON nc.concept_id = n.note_type_concept_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = n.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
