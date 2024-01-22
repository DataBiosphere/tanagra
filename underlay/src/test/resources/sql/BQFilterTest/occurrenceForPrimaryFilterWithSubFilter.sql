
    SELECT
        id      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        person_id IN (
            SELECT
                id              
            FROM
                ${ENT_person}              
            WHERE
                (
                    id IN (
                        SELECT
                            person_id AS primary_id                          
                        FROM
                            ${ENT_conditionOccurrence}                          
                        WHERE
                            condition IN (
                                @val0,@val1                             
                            )                     
                    )                 
                )                  
                AND (
                    gender = @val2                 
                )             
            )
