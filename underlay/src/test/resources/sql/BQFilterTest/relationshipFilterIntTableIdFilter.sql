
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
                entity_A_id = @val         
        )
