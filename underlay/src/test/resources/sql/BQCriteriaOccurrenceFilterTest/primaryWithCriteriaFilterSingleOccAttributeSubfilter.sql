
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (SELECT
            person_id AS primary_id          
        FROM
            ${ENT_conditionOccurrence}          
        WHERE
            (condition = @val0)              
            AND (age_at_occurrence BETWEEN @val1 AND @val2))
