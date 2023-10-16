SELECT
  mo.measurement_id AS id, mo.person_id,
  CAST(CASE
    WHEN xvw.x_invalid = 'N' THEN 1
    ELSE 0
  END AS BOOLEAN) AS is_clean,
  mo.measurement_date,
  mo.value_as_number AS value_numeric, mo.value_as_concept_id, mo.unit_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(mo.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  mo.visit_occurrence_id, vo.visit_concept_id

FROM `sd-vumc-tanagra-test.sd_20230331.measurement` AS mo

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.x_vs_wh` AS xvw
ON xvw.measurement_id = mo.measurement_id

JOIN `sd-vumc-tanagra-test.sd_20230331.person` AS p
ON p.person_id = mo.person_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.visit_occurrence` AS vo
ON vo.visit_occurrence_id = mo.visit_occurrence_id

WHERE mo.measurement_type_concept_id = 44818701
    AND (mo.measurement_source_value = 'HEIGHT'
        OR mo.measurement_concept_id in (3035463, 3036277))
