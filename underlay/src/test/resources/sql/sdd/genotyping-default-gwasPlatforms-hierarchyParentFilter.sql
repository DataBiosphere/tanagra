
    SELECT
        (t.T_PATH_default IS NOT NULL) AS T_ISMEM_default,
        (t.T_PATH_default IS NOT NULL 
        AND t.T_PATH_default='') AS T_ISRT_default,
        t.T_NUMCH_default AS T_NUMCH_default,
        t.T_PATH_default AS T_PATH_default,
        t.id AS id,
        t.name AS name 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_genotyping AS t 
    WHERE
        t.id IN (
            SELECT
                t.child 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.T_HCP_genotyping_default AS t 
            WHERE
                t.parent = 101
        ) LIMIT 30
