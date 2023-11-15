
    SELECT
        (e.T_PATH_default IS NOT NULL) AS T_ISMEM_default,
        (e.T_PATH_default IS NOT NULL 
        AND e.T_PATH_default='') AS T_ISRT_default,
        e.T_NUMCH_default AS T_NUMCH_default,
        e.T_PATH_default AS T_PATH_default,
        e.concept_code AS concept_code,
        e.id AS id,
        e.name AS name,
        e.standard_concept AS standard_concept,
        e.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_procedure AS e 
    WHERE
        (
            e.id IN (
                SELECT
                    h.descendant 
                FROM
                    `verily-tanagra-dev.cmssynpuf_index_110623`.HAD_procedure_default AS h 
                WHERE
                    h.ancestor = 4176720
            ) 
            OR e.id = 4176720
        ) LIMIT 30
