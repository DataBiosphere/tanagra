SELECT
    z.zip3, z.zip3_as_string, z.fraction_assisted_income
    ,z.fraction_high_school_edu, z.median_income,
    z.fraction_no_heath_ins, z.fraction_poverty,
    z.fraction_vacant_housing, z.deprivation_index, z.acs
FROM `all-of-us-ehr-dev.SC2022Q4R6.zip3_ses_map` z

