
    SELECT
        e.id AS id 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_person AS e 
    WHERE
        e.id IN (
            SELECT
                r.entity_B_id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.RIDS_snpPerson_snp_person AS r 
            WHERE
                r.entity_A_id IN (
                    SELECT
                        e.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.ENT_snp AS e 
                    WHERE
                        e.name = 'RS12925749'
                )
            ) LIMIT 30
