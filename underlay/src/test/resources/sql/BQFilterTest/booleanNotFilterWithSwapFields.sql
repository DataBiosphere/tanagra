
    SELECT
        person_id      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        NOT condition IN (
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
