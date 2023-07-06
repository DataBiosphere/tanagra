SELECT o.person_id,
       o.observation_date,
       zip.zip3_as_string,
       zip.fraction_assisted_income,
       zip.fraction_high_school_edu,
       zip.median_income,
       zip.fraction_no_health_ins,
       zip.fraction_poverty,
       zip.fraction_vacant_housing,
       zip.deprivation_index,
       zip.acs
FROM `all-of-us-ehr-dev.SC2022Q4R6.observation` AS o
JOIN `all-of-us-ehr-dev.SC2022Q4R6.zip3_ses_map` zip
ON CAST(SUBSTR(o.value_as_string, 0, STRPOS(o.value_as_string, '*') - 1) as INT64) = zip.zip3
WHERE observation_source_concept_id = 1585250
AND o.value_as_string not like 'Res%'
