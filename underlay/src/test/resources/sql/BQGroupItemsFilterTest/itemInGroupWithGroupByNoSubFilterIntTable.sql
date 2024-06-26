
    SELECT
        name      
    FROM
        ${ENT_ingredient}      
    WHERE
        id IN (SELECT
            entity_B_id          
        FROM
            ${RIDS_brandIngredient_brand_ingredient}          
        GROUP BY
            entity_B_id          
        HAVING
            COUNT(*) > @groupByCount0)
