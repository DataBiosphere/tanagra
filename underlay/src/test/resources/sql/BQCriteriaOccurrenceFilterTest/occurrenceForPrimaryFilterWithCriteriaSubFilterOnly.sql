
    SELECT
        id      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        condition IN (
            SELECT
                descendant              
            FROM
                ${HAD_condition_default}              
            WHERE
                ancestor IN (
                    @val0,@val1                 
                )              
            UNION
            ALL SELECT
                @val2              
            UNION
            ALL SELECT
                @val3         
        )
