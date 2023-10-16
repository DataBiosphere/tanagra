SELECT
  ROW_NUMBER() OVER (ORDER BY xfh.uniq) AS id, xfh.person_id,
  xfh.entry_date, xfh.item AS note_text,
  CAST(FLOOR(TIMESTAMP_DIFF(xfh.entry_date, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  xfh.visit_occurrence_id, vo.visit_concept_id

FROM `sd-vumc-tanagra-test.sd_20230331.x_family_history` AS xfh

JOIN `sd-vumc-tanagra-test.sd_20230331.person` AS p
ON p.person_id = xfh.person_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.visit_occurrence` AS vo
ON vo.visit_occurrence_id = xfh.visit_occurrence_id