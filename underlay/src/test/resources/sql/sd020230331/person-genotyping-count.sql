
    SELECT
        COUNT(e.id) AS T_IDCT,
        e.gender AS gender,
        e.race AS race 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_person AS e 
    WHERE
        e.id IN (
            SELECT
                r.entity_B_id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.RIDS_genotypingPerson_genotyping_person AS r 
            WHERE
                r.entity_A_id IN (
                    SELECT
                        e.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.ENT_genotyping AS e 
                    WHERE
                        e.name = 'Illumina 5M'
                )
            ) 
        GROUP BY
            gender,
            race
