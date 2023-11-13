
    SELECT
        (e.T_PATH_default IS NOT NULL) AS T_ISMEM_default,
        (e.T_PATH_default IS NOT NULL 
        AND e.T_PATH_default='') AS T_ISRT_default,
        e.T_NUMCH_default AS T_NUMCH_default,
        e.T_PATH_default AS T_PATH_default,
        e.id AS id,
        e.name AS name 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_genotyping AS e 
    WHERE
        e.id IN (
            SELECT
                h.child 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.HCP_genotyping_default AS h 
            WHERE
                h.parent = 101
        ) LIMIT 30
