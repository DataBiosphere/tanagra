
    SELECT
        year_of_birth      
    FROM
        ${ENT_person}      
    WHERE
        id IN (
            SELECT
                person_id              
            FROM
                ${ENT_conditionOccurrence}              
            WHERE
                person_id = @val0              
            GROUP BY
                person_id,
                start_date              
            HAVING
                COUNT(*) > @groupByCount0         
        )
