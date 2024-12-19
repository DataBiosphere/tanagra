SELECT p.person_id,
       p.year_of_birth,
       p.birth_datetime,
       CASE
           WHEN d.death_date IS NULL THEN p.birth_datetime END birth_datetime_not_deceased,
       p.gender_concept_id,
       CASE
           WHEN gc.concept_name = 'Male' THEN 'Man'
           WHEN gc.concept_name = 'Female' THEN 'Woman'
           ELSE gc.concept_name
           END AS gender_concept_name,
       p.race_concept_id,
       CASE
           WHEN rc.concept_name = 'No matching concept' THEN 'Unknown'
           WHEN rc.concept_name = 'PMI: Skip' THEN 'Skip'
           ELSE rc.concept_name
           END AS race_concept_name,
       p.ethnicity_concept_id,
       CASE
           WHEN ec.concept_name = 'No matching concept' THEN 'Unknown'
           ELSE ec.concept_name
           END AS ethnicity_concept_name,
       p.sex_at_birth_concept_id,
       sc.concept_name AS sex_at_birth_concept_name,
       p.self_reported_category_concept_id,
       sr.concept_name as self_reported_category,
       CASE
           WHEN asum.person_id IS NULL THEN FALSE ELSE TRUE END has_fitbit_activity_summary,
       CASE
           WHEN hrml.person_id IS NULL THEN FALSE ELSE TRUE END has_fitbit_heart_rate_level,
       CASE
           WHEN hrs.person_id IS NULL THEN FALSE ELSE TRUE END has_fitbit_heart_rate_summary,
       CASE
           WHEN si.person_id IS NULL THEN FALSE ELSE TRUE END has_fitbit_steps_intraday,
       CASE
           WHEN sds.person_id IS NULL THEN FALSE ELSE TRUE END has_fitbit_sleep_daily_summary,
       CASE
           WHEN sl.person_id IS NULL THEN FALSE ELSE TRUE END has_fitbit_sleep_level,
       CASE
           WHEN asum.person_id IS NULL AND hrml.person_id IS NULL AND hrs.person_id IS NULL
               AND si.person_id IS NULL AND sds.person_id IS NULL AND sl.person_id IS NULL THEN FALSE ELSE TRUE END has_fitbit,
       CASE
           WHEN ws.person_id IS NULL THEN FALSE ELSE TRUE END has_wear_consent,
       CASE
           WHEN ehr.person_id IS NULL THEN FALSE ELSE TRUE END has_ehr_data,
       CASE
           WHEN pm.person_id IS NULL THEN FALSE ELSE TRUE END has_pm_data,
       CASE
           WHEN d.death_date is null THEN FALSE ELSE TRUE END is_deceased
FROM `${omopDataset}.person` p
         LEFT JOIN `${omopDataset}.concept` gc ON gc.concept_id = p.gender_concept_id
         LEFT JOIN `${omopDataset}.concept` rc ON rc.concept_id = p.race_concept_id
         LEFT JOIN `${omopDataset}.concept` ec ON ec.concept_id = p.ethnicity_concept_id
         LEFT JOIN `${omopDataset}.concept` sc ON sc.concept_id = p.sex_at_birth_concept_id
         LEFT JOIN `${omopDataset}.concept` sr ON sr.concept_id = p.self_reported_category_concept_id
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.activity_summary`) asum ON (p.person_id = asum.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.heart_rate_minute_level`) hrml ON (p.person_id = hrml.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.heart_rate_summary`) hrs ON (p.person_id = hrs.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.steps_intraday`) si ON (p.person_id = si.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.sleep_daily_summary`) sds ON (p.person_id = sds.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.sleep_level`) sl ON (p.person_id = sl.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.wear_study`) ws ON (p.person_id = ws.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM`${omopDataset}.measurement` as a
                                                      LEFT JOIN`${omopDataset}.measurement_ext` as b on a.measurement_id = b.measurement_id
                    WHERE lower(b.src_id) like 'ehr site%'
                    UNION DISTINCT
                    SELECT DISTINCT person_id FROM`${omopDataset}.condition_occurrence` as a
                        LEFT JOIN`${omopDataset}.condition_occurrence_ext` as b on a.condition_occurrence_id = b.condition_occurrence_id
                    WHERE lower(b.src_id) like 'ehr site%'
                    UNION DISTINCT
                    SELECT DISTINCT person_id FROM`${omopDataset}.device_exposure` as a
                        LEFT JOIN`${omopDataset}.device_exposure_ext` as b on a.device_exposure_id = b.device_exposure_id
                    WHERE lower(b.src_id) like 'ehr site%'
                    UNION DISTINCT
                    SELECT DISTINCT person_id FROM`${omopDataset}.drug_exposure` as a
                        LEFT JOIN`${omopDataset}.drug_exposure_ext` as b on a.drug_exposure_id = b.drug_exposure_id
                    WHERE lower(b.src_id) like 'ehr site%'
                    UNION DISTINCT
                    SELECT DISTINCT person_id FROM`${omopDataset}.observation` as a
                        LEFT JOIN`${omopDataset}.observation_ext` as b on a.observation_id = b.observation_id
                    WHERE lower(b.src_id) like 'ehr site%'
                    UNION DISTINCT
                    SELECT DISTINCT person_id FROM`${omopDataset}.procedure_occurrence` as a
                        LEFT JOIN`${omopDataset}.procedure_occurrence_ext` as b on a.procedure_occurrence_id = b.procedure_occurrence_id
                    WHERE lower(b.src_id) like 'ehr site%'
                    UNION DISTINCT
                    SELECT DISTINCT person_id FROM`${omopDataset}.visit_occurrence` as a
                        LEFT JOIN`${omopDataset}.visit_occurrence_ext` as b on a.visit_occurrence_id = b.visit_occurrence_id
                    WHERE lower(b.src_id) like 'ehr site%'
) ehr ON (p.person_id = ehr.person_id)
         LEFT JOIN (SELECT DISTINCT person_id FROM `${omopDataset}.measurement`
                    WHERE measurement_source_concept_id in (
                        SELECT concept_id FROM `${omopDataset}.concept`
                        WHERE vocabulary_id = 'PPI'
                          AND concept_class_id = 'Clinical Observation'
                          AND domain_id = 'Measurement'
                    )
) pm ON (p.person_id = pm.person_id)
         LEFT JOIN (SELECT person_id, max(death_date) as death_date FROM `${omopDataset}.death` GROUP BY person_id) d
                   ON (p.person_id = d.person_id)
