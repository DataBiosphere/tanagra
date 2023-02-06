/* Add age_at_event column */
SELECT
    /* Can't do "*". During expansion, there's an error about person_id column being ambiguous. */
    v.visit_occurrence_id, v.visit_concept_id, v.person_id, v.visit_start_date, v.visit_end_date, v.visit_source_value, v.visit_source_concept_id,

    /* https://stackoverflow.com/a/70433423/6447189 */
    DATE_DIFF(DATE(v.visit_start_datetime), DATE(p.birth_datetime), YEAR) -
    IF(EXTRACT(MONTH FROM DATE(p.birth_datetime))*100 + EXTRACT(DAY FROM DATE(p.birth_datetime)) >
      EXTRACT(MONTH FROM DATE(v.visit_start_datetime))*100 + EXTRACT(DAY FROM DATE(v.visit_start_datetime))
      ,1,0) AS age_at_event
FROM
    `victr-tanagra-test.sd_static.visit_occurrence` v,
    `victr-tanagra-test.sd_static.person` p
WHERE v.person_id = p.person_id
