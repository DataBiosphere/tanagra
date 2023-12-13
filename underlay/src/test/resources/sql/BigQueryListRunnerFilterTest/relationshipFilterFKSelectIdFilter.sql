
    SELECT
        c.start_date      
    FROM
        ${ENT_conditionOccurrence} AS c      
    WHERE
        c.person_id = @val
