
    SELECT
        name      
    FROM
        ${ENT_ingredient}      
    WHERE
        id IN (
            SELECT
                it.entity_B_id              
            FROM
                ${RIDS_brandIngredient_brand_ingredient} it              
            JOIN
                ${ENT_brand} fe                      
                    ON fe.id = it.entity_A_id              
            WHERE
                fe.standard_concept = @val1              
            GROUP BY
                entity_B_id              
            HAVING
                COUNT(DISTINCT fe.vocabulary) > @groupByCount2         
        )
