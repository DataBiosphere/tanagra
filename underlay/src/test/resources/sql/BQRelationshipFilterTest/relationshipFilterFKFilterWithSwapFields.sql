
    SELECT
        start_date      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        person_id IN (SELECT
            person_id          
        FROM
            ${ENT_conditionOccurrence}          
        WHERE
            condition = @val0)
