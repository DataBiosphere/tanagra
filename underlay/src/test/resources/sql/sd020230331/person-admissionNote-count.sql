
    SELECT
        COUNT(e.id) AS T_IDCT,
        e.gender AS gender,
        e.race AS race 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_person AS e 
    WHERE
        e.id IN (
            SELECT
                e.person_id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.ENT_noteOccurrence AS e 
            WHERE
                e.note IN (
                    SELECT
                        e.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.ENT_note AS e 
                    WHERE
                        e.id = 44814638
                )
            ) 
        GROUP BY
            gender,
            race
