SELECT p.person_id,
       p.year_of_birth,
       p.birth_datetime,
       p.gender_concept_id,
       p.race_concept_id,
       p.ethnicity_concept_id,
       p.sex_at_birth_concept_id,
       CASE
           WHEN asum.person_id IS NULL THEN 0 ELSE 1 END has_fitbit_activity_summary,
       CASE
           WHEN hrml.person_id IS NULL THEN 0 ELSE 1 END has_fitbit_heart_rate_level,
       CASE
           WHEN hrs.person_id IS NULL THEN 0 ELSE 1 END has_fitbit_heart_rate_summary,
       CASE
           WHEN si.person_id IS NULL THEN 0 ELSE 1 END has_fitbit_steps_intraday,
       CASE
           WHEN sds.person_id IS NULL THEN 0 ELSE 1 END has_fitbit_sleep_daily_summary,
       CASE
           WHEN sl.person_id IS NULL THEN 0 ELSE 1 END has_fitbit_sleep_level,
       CASE
           WHEN asum.person_id IS NULL AND hrml.person_id IS NULL AND hrs.person_id IS NULL
            AND si.person_id IS NULL AND sds.person_id IS NULL AND sl.person_id IS NULL THEN 0 ELSE 1 END has_fitbit,
       CASE
           WHEN ehr.person_id IS NULL THEN 0 ELSE 1 END has_ehr_data
FROM `all-of-us-ehr-dev.SR2023Q3R1.person` p
         LEFT JOIN
     (SELECT DISTINCT person_id
      FROM `all-of-us-ehr-dev.SR2023Q3R1.activity_summary`) asum ON (p.person_id = asum.person_id)
         LEFT JOIN
     (SELECT DISTINCT person_id
      FROM `all-of-us-ehr-dev.SR2023Q3R1.heart_rate_minute_level`) hrml ON (p.person_id = hrml.person_id)
         LEFT JOIN
     (SELECT DISTINCT person_id
      FROM `all-of-us-ehr-dev.SR2023Q3R1.heart_rate_summary`) hrs ON (p.person_id = hrs.person_id)
         LEFT JOIN
     (SELECT DISTINCT person_id
      FROM `all-of-us-ehr-dev.SR2023Q3R1.steps_intraday`) si ON (p.person_id = si.person_id)
         LEFT JOIN
     (SELECT DISTINCT person_id
      FROM `all-of-us-ehr-dev.SR2023Q3R1.sleep_daily_summary`) sds ON (p.person_id = sds.person_id)
         LEFT JOIN
     (SELECT DISTINCT person_id
      FROM `all-of-us-ehr-dev.SR2023Q3R1.sleep_level`) sl ON (p.person_id = sl.person_id)
         LEFT JOIN
     (SELECT DISTINCT person_id
      FROM`all-of-us-ehr-dev.SR2023Q3R1.measurement` as a
              LEFT JOIN`all-of-us-ehr-dev.SR2023Q3R1.measurement_ext` as b on a.measurement_id = b.measurement_id
      WHERE lower(b.src_id) like 'ehr site%'
      UNION DISTINCT
      SELECT DISTINCT person_id
      FROM`all-of-us-ehr-dev.SR2023Q3R1.condition_occurrence` as a
          LEFT JOIN`all-of-us-ehr-dev.SR2023Q3R1.condition_occurrence_ext` as b on a.condition_occurrence_id = b.condition_occurrence_id
      WHERE lower(b.src_id) like 'ehr site%'
      UNION DISTINCT
      SELECT DISTINCT person_id
      FROM`all-of-us-ehr-dev.SR2023Q3R1.device_exposure` as a
          LEFT JOIN`all-of-us-ehr-dev.SR2023Q3R1.device_exposure_ext` as b on a.device_exposure_id = b.device_exposure_id
      WHERE lower(b.src_id) like 'ehr site%'
      UNION DISTINCT
      SELECT DISTINCT person_id
      FROM`all-of-us-ehr-dev.SR2023Q3R1.drug_exposure` as a
          LEFT JOIN`all-of-us-ehr-dev.SR2023Q3R1.drug_exposure_ext` as b on a.drug_exposure_id = b.drug_exposure_id
      WHERE lower(b.src_id) like 'ehr site%'
      UNION DISTINCT
      SELECT DISTINCT person_id
      FROM`all-of-us-ehr-dev.SR2023Q3R1.observation` as a
          LEFT JOIN`all-of-us-ehr-dev.SR2023Q3R1.observation_ext` as b on a.observation_id = b.observation_id
      WHERE lower(b.src_id) like 'ehr site%'
      UNION DISTINCT
      SELECT DISTINCT person_id
      FROM`all-of-us-ehr-dev.SR2023Q3R1.procedure_occurrence` as a
          LEFT JOIN`all-of-us-ehr-dev.SR2023Q3R1.procedure_occurrence_ext` as b on a.procedure_occurrence_id = b.procedure_occurrence_id
      WHERE lower(b.src_id) like 'ehr site%'
      UNION DISTINCT
      SELECT DISTINCT person_id
      FROM`all-of-us-ehr-dev.SR2023Q3R1.visit_occurrence` as a
          LEFT JOIN`all-of-us-ehr-dev.SR2023Q3R1.visit_occurrence_ext` as b on a.visit_occurrence_id = b.visit_occurrence_id
      WHERE lower(b.src_id) like 'ehr site%'
     ) ehr ON (p.person_id = ehr.person_id)
