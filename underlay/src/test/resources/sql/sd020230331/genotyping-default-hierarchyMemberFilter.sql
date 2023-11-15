
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
        e.T_PATH_default IS NOT NULL LIMIT 30
