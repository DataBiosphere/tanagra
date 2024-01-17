
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        id IN (
            SELECT
                descendant              
            FROM
                ${HAD_condition_default}              
            WHERE
                ancestor = @val              
            UNION
            ALL SELECT
                @val0         
        )
