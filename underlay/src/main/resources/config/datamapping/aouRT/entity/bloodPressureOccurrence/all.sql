WITH blood_pressure AS (
    SELECT person_id,
           measurement_datetime,
           CASE
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) BETWEEN 121 AND 139) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) BETWEEN 81 AND 89) THEN '1'
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) >= 140) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) >= 90) THEN '2'
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) <= 90) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) <= 60) THEN '3'
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) BETWEEN 91 AND 120) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) BETWEEN 61 AND 80) THEN '4'
               END AS status_code,
           CASE
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) BETWEEN 121 AND 139) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) BETWEEN 81 AND 89) THEN 'Pre Hypertensive'
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) >= 140) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) >= 90) THEN 'Hypertensive'
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) <= 90) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) <= 60) THEN 'Hypotensive'
               WHEN (CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) BETWEEN 91 AND 120) AND (CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) BETWEEN 61 AND 80) THEN 'Normal'
               ELSE 'Unknown Range'
               END AS status_code_name,
           CAST(SPLIT(values, ',')[offset(0)] AS FLOAT64) AS systolic,
           CAST(SPLIT(values, ',')[offset(1)] AS FLOAT64) AS diastolic
    FROM (
             SELECT person_id,
                    measurement_datetime,
                    STRING_AGG(CAST(value_as_number AS STRING)) AS values
             FROM (
                 SELECT person_id,
                 measurement_datetime,
                 value_as_number
                 FROM `${omopDataset}.measurement`
                 WHERE measurement_source_concept_id IN (903118, 903115)
                 ORDER BY person_id ASC,
                 measurement_datetime ASC,
                 measurement_source_concept_id DESC
                 )
             GROUP BY person_id,
                 measurement_datetime
         )
    ORDER BY person_id ASC,
             measurement_datetime ASC
)
SELECT m.measurement_id,
       bp.person_id,
       bp.measurement_datetime,
       bp.systolic,
       bp.diastolic,
       bp.status_code,
       bp.status_code_name,
       CAST(FLOOR(TIMESTAMP_DIFF(bp.measurement_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
       m.visit_occurrence_id,
       vo.visit_concept_id,
       vc.concept_name AS visit_concept_name
FROM blood_pressure bp
         JOIN `${omopDataset}.person` AS p ON p.person_id = bp.person_id
         JOIN `${omopDataset}.measurement` AS m ON (m.person_id = bp.person_id AND m.measurement_datetime = bp.measurement_datetime AND m.measurement_source_concept_id IN (903118))
         LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = m.visit_occurrence_id
         LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id