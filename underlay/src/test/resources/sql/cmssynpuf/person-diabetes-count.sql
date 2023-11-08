
    SELECT
        COUNT(t.id) AS T_IDCT,
        t.gender AS gender,
        t.race AS race 
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
            ) 
        GROUP BY
            gender,
            race
