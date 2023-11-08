
    SELECT
        (t.T_PATH_default IS NOT NULL) AS T_ISMEM_default,
        (t.T_PATH_default IS NOT NULL 
        AND t.T_PATH_default='') AS T_ISRT_default,
        t.T_NUMCH_default AS T_NUMCH_default,
        t.T_PATH_default AS T_PATH_default,
        t.concept_code AS concept_code,
        t.id AS id,
        t.name AS name,
        t.standard_concept AS standard_concept,
        t.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.T_ENT_condition AS t 
    WHERE
        (
            t.id IN (
                SELECT
                    t.descendant 
                FROM
                    `verily-tanagra-dev.cmssynpuf_index_110623`.T_HAD_condition_default AS t 
                WHERE
                    t.ancestor = 201826
            ) 
            OR t.id = 201826
        ) LIMIT 30
