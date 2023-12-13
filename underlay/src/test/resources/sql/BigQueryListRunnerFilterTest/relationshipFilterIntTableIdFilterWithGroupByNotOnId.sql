
    SELECT
        i.name      
    FROM
        ${ENT_ingredient} AS i      
    WHERE
        i.id IN (
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
                        id = @val                      
                    GROUP BY
                        concept_code                      
                    HAVING
                        COUNT(*) > @groupByCount                 
                )             
            )
