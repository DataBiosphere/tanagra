
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
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_condition AS t 
    WHERE
        t.id IN (
            SELECT
                t.child 
            FROM
                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_HCP_condition_default AS t 
            WHERE
                t.parent = 201826
        ) LIMIT 30
