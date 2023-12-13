
    SELECT
        c.start_date      
    FROM
        ${ENT_conditionOccurrence} AS c      
    WHERE
        c.person_id IN (
            SELECT
                id              
            FROM
                ${ENT_person}              
            WHERE
                id = @val              
            GROUP BY
                id              
            HAVING
                COUNT(*) > @groupByCount         
        )
