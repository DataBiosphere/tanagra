
    SELECT
        name      
    FROM
        ${ENT_ingredient}      
    WHERE
        id IN (
            SELECT
                entity_B_id              
            FROM
                ${RIDS_brandIngredient_brand_ingredient}              
            WHERE
                entity_A_id IN (
                    SELECT
                        id                      
                    FROM
                        ${ENT_brand}                      
                    WHERE
                        concept_code = @val                 
                )              
            GROUP BY
                entity_B_id              
            HAVING
                COUNT(*) = @groupByCount             
            )
