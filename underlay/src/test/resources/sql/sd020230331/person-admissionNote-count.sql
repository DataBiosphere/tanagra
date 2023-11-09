
    SELECT
        COUNT(t.id) AS T_IDCT,
        t.gender AS gender,
        t.race AS race 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_person AS t 
    WHERE
        t.id IN (
            SELECT
                t.person_id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_noteOccurrence AS t 
            WHERE
                t.note IN (
                    SELECT
                        t.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_note AS t 
                    WHERE
                        t.id = 44814638
                )
            ) 
        GROUP BY
            gender,
            race
