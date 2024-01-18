
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
                ancestor IN (
                    @val,@val0                 
                )              
            UNION
            ALL SELECT
                @val1              
            UNION
            ALL SELECT
                @val2         
        )
