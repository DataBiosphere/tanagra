SELECT
    observation.person_id,
    observation.observation_datetime,
    zip_code.zip3_as_string as zip_code,
    zip_code.fraction_assisted_income as assisted_income,
    zip_code.fraction_high_school_edu as high_school_education,
    zip_code.median_income,
    zip_code.fraction_no_health_ins as no_health_insurance,
    zip_code.fraction_poverty as poverty,
    zip_code.fraction_vacant_housing as vacant_housing,
    zip_code.deprivation_index,
    zip_code.acs as american_community_survey_year
FROM
    `all-of-us-ehr-dev.SC2023Q3R1.zip3_ses_map` zip_code
JOIN
    `all-of-us-ehr-dev.SC2023Q3R1..observation` observation
        ON CAST(SUBSTR(observation.value_as_string,
            0, STRPOS(observation.value_as_string,'*') - 1) AS INT64) = zip_code.zip3
    AND observation_source_concept_id = 1585250
    AND observation.value_as_string NOT LIKE 'Res%'
