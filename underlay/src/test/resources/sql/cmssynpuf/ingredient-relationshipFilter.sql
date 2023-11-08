
    SELECT
        t.concept_code AS concept_code,
        t.id AS id,
        t.name AS name,
        t.standard_concept AS standard_concept,
        t.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.T_ENT_ingredient AS t 
    WHERE
        t.id IN (
            SELECT
                t.entity_B_id 
            FROM
                `verily-tanagra-dev.cmssynpuf_index_110623`.T_RIDS_brandIngredient_brand_ingredient AS t 
            WHERE
                t.entity_A_id IN (
                    SELECT
                        t.id 
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_110623`.T_ENT_brand AS t 
                    WHERE
                        t.id = 19082059
                )
            ) LIMIT 30
