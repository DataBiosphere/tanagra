
    SELECT
        e.concept_code AS concept_code,
        e.id AS id,
        e.name AS name,
        e.standard_concept AS standard_concept,
        e.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_ingredient AS e 
    WHERE
        e.id IN (
            SELECT
                r.entity_B_id 
            FROM
                `verily-tanagra-dev.cmssynpuf_index_110623`.RIDS_brandIngredient_brand_ingredient AS r 
            WHERE
                r.entity_A_id IN (
                    SELECT
                        e.id 
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_brand AS e 
                    WHERE
                        e.id = 19082059
                )
            ) LIMIT 30
