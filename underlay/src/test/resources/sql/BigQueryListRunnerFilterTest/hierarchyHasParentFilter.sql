
    SELECT
        c.name      
    FROM
        ${ENT_condition} AS c      
    WHERE
        c.id IN (
            SELECT
                child              
            FROM
                ${HCP_condition_default}              
            WHERE
                parent = @val         
        )
