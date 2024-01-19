
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        id IN (
            SELECT
                child              
            FROM
                ${HCP_condition_default}              
            WHERE
                parent = @val0
        )
