
    SELECT
        start_date      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        person_id IN (
            SELECT
                id              
            FROM
                ${ENT_person}              
            WHERE
                gender = @val0
        )
