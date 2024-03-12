
    SELECT
        name      
    FROM
        ${ENT_ingredient}      
    WHERE
        id IN (
            SELECT
                entity_B_id              
            FROM
                (SELECT
                    it.entity_B_id                  
                FROM
                    ${RIDS_brandIngredient_brand_ingredient} AS it                  
                JOIN
                    ${ENT_brand} AS fe                          
                        ON fe.id = it.entity_A_id                  
                WHERE
                    fe.standard_concept = @val1                  
                GROUP BY
                    entity_B_id,
                    fe.vocabulary)              
            GROUP BY
                entity_B_id              
            HAVING
                COUNT(*) > @groupByCount2             
            )
