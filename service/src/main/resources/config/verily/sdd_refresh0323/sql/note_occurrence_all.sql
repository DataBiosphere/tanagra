SELECT
  n.note_id AS id, n.person_id, n.note_type_concept_id,
  n.note_date, n.note_title AS title, n.note_text AS text,
  n.note_source_value AS source_value,
  CAST(FLOOR(TIMESTAMP_DIFF(n.note_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  n.visit_occurrence_id, vo.visit_concept_id

FROM `sd-vumc-tanagra-test.sd_20230331.note` AS n

JOIN `sd-vumc-tanagra-test.sd_20230331.person` AS p
ON p.person_id = n.person_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.visit_occurrence` AS vo
ON vo.visit_occurrence_id = n.visit_occurrence_id
