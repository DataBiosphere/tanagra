
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
                entity_A_id = @val              
            GROUP BY
                entity_A_id              
            HAVING
                COUNT(*) = @groupByCount         
        )
