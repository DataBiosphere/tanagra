
    SELECT
        person_id      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        (
            condition IN (
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
    )      
    OR (
        condition IN (
            SELECT
                descendant              
            FROM
                ${HAD_condition_default}              
            WHERE
                ancestor = @val1              
            UNION
            ALL SELECT
                @val2         
        ) 
)
