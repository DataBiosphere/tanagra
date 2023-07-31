SELECT
    /* Can't do "*". During expansion, there's an error about person_id column being ambiguous. */
    p.person_id, p.year_of_birth, DATETIME(p.year_of_birth, p.month_of_birth, p.day_of_birth, 0, 0, 0), p.gender_concept_id, p.race_concept_id, p.ethnicity_concept_id
FROM `bigquery-public-data.cms_synthetic_patient_data_omop.person` p
