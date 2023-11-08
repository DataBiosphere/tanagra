
    SELECT
        COUNT(t.id) AS T_IDCT,
        t.gender AS gender,
        t.race AS race 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_person AS t 
    WHERE
        t.id IN (
            SELECT
                t.entity_B_id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.T_RIDS_genotypingPerson_genotyping_person AS t 
            WHERE
                t.entity_A_id IN (
                    SELECT
                        t.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_genotyping AS t 
                    WHERE
                        t.name = 'Illumina 5M'
                )
            ) 
        GROUP BY
            gender,
            race
