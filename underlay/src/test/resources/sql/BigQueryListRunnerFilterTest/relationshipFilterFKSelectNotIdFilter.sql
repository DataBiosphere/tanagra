
    SELECT
        c.start_date      
    FROM
        ${ENT_conditionOccurrence} AS c      
    WHERE
        c.condition IN (
            SELECT
                id              
            FROM
                ${ENT_condition}              
            WHERE
                concept_code = @val         
        )
