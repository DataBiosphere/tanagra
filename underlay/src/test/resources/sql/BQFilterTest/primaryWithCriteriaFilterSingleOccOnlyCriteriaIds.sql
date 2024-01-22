
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (
            SELECT
                person_id AS primary_id              
            FROM
                ${ENT_conditionOccurrence}              
            WHERE
                condition IN (
                    @val,@val0                 
                )         
        )
