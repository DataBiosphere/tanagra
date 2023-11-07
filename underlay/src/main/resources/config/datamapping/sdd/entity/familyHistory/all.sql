SELECT
  ROW_NUMBER() OVER (ORDER BY xfh.uniq) AS id, 
  xfh.person_id,
  xfh.entry_date, 
  xfh.item AS note_text, 
  xfh.family_history_src_name, 
  xfh.relation_name,
  CAST(FLOOR(TIMESTAMP_DIFF(xfh.entry_date, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  xfh.visit_occurrence_id, 
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.x_family_history` AS xfh

JOIN `${omopDataset}.person` AS p
    ON p.person_id = xfh.person_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = xfh.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
