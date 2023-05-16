SELECT
  n.note_id AS id, n.person_id, n.note_type_concept_id,
  n.note_date, n.note_title AS title, n.note_text AS text,
  n.note_source_value AS source_value,
  CAST(FLOOR(TIMESTAMP_DIFF(n.note_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  n.visit_occurrence_id, vo.visit_concept_id

FROM `victr-tanagra-test.sd_static.note` AS n

JOIN `victr-tanagra-test.sd_static.person` AS p
ON p.person_id = n.person_id

LEFT JOIN `victr-tanagra-test.sd_static.visit_occurrence` AS vo
ON vo.visit_occurrence_id = n.visit_occurrence_id
