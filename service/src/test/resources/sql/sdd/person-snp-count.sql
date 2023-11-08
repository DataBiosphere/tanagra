
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
                `verily-tanagra-dev.sd20230331_index_110623`.T_RIDS_snpPerson_snp_person AS t 
            WHERE
                t.entity_A_id IN (
                    SELECT
                        t.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_snp AS t 
                    WHERE
                        t.name = 'RS12925749'
                )
            ) 
        GROUP BY
            gender,
            race
