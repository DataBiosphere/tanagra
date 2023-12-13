
    SELECT
        c.name      
    FROM
        ${ENT_condition} AS c      
    WHERE
        c.id IN (
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
