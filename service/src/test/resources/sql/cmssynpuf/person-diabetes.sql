
    SELECT
        t.T_DISP_ethnicity AS T_DISP_ethnicity,
        t.T_DISP_gender AS T_DISP_gender,
        t.T_DISP_race AS T_DISP_race,
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        t.age,
        DAY) / 365.25) AS INT64) AS age,
        t.ethnicity AS ethnicity,
        t.gender AS gender,
        t.id AS id,
        t.person_source_value AS person_source_value,
        t.race AS race,
        t.year_of_birth AS year_of_birth 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.T_ENT_person AS t 
    WHERE
        t.id IN (
            SELECT
                t.person_id 
            FROM
                `verily-tanagra-dev.cmssynpuf_index_110623`.T_ENT_conditionOccurrence AS t 
            WHERE
                t.condition IN (
                    SELECT
                        t.id 
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_110623`.T_ENT_condition AS t 
                    WHERE
                        t.id = 201826
                )
            ) LIMIT 30
