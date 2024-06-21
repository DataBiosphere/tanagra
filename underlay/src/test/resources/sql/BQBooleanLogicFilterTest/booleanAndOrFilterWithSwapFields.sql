
    SELECT
        person_id      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        (
            condition IN (SELECT
                descendant              
            FROM
                ${HAD_condition_default}              
            WHERE
                ancestor = @val0              
            UNION
            ALL SELECT
                @val1)         
        )          
        OR (
            condition IN (SELECT
                descendant              
            FROM
                ${HAD_condition_default}              
            WHERE
                ancestor = @val2              
            UNION
            ALL SELECT
                @val3)         
        )
