
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
                gender = @val              
            GROUP BY
                id              
            HAVING
                COUNT(*) > @groupByCount         
        )
