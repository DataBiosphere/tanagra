
    SELECT
        COUNT(e.id) AS T_IDCT,
        e.gender AS gender,
        e.race AS race 
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
            ) 
        GROUP BY
            gender,
            race
