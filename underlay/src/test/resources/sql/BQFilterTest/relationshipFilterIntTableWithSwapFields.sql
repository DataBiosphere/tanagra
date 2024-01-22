
    SELECT
        start_date      
    FROM
        ${ENT_ingredientOccurrence}      
    WHERE
        ingredient IN (
            SELECT
                entity_B_id              
            FROM
                ${RIDS_brandIngredient_brand_ingredient}              
            WHERE
                entity_A_id = @val0         
        )
