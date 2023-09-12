SELECT
    p.person_id, p.year_of_birth, p.birth_datetime, p.gender_concept_id,
    p.race_concept_id, p.ethnicity_concept_id, p.sex_at_birth_concept_id,
    EXISTS
        (SELECT 1 FROM `all-of-us-ehr-dev.SR2023Q3R1.activity_summary` fas WHERE p.person_id = fas.person_id)
        AS has_fitbit_activity_summary
FROM `all-of-us-ehr-dev.SR2023Q3R1.person` p
