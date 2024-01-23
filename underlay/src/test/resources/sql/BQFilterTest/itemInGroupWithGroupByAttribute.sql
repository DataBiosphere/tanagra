
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
            GROUP BY
                entity_B_id,
                fe.id,
                fe.vocabulary              
            HAVING
                COUNT(*) > @groupByCount1         
        )
