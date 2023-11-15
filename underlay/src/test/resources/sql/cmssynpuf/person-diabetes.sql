
    SELECT
        e.T_DISP_ethnicity AS T_DISP_ethnicity,
        e.T_DISP_gender AS T_DISP_gender,
        e.T_DISP_race AS T_DISP_race,
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        e.age,
        DAY) / 365.25) AS INT64) AS age,
        e.ethnicity AS ethnicity,
        e.gender AS gender,
        e.id AS id,
        e.person_source_value AS person_source_value,
        e.race AS race,
        e.year_of_birth AS year_of_birth 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_person AS e 
    WHERE
        e.id IN (
            SELECT
                e.person_id 
            FROM
                `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_conditionOccurrence AS e 
            WHERE
                e.condition IN (
                    SELECT
                        e.id 
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_condition AS e 
                    WHERE
                        e.id = 201826
                )
            ) LIMIT 30
