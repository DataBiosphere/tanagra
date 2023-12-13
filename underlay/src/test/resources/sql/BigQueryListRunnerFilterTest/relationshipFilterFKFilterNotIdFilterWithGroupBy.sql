
    SELECT
        p.year_of_birth      
    FROM
        ${ENT_person} AS p      
    WHERE
        p.id IN (
            SELECT
                person_id              
            FROM
                ${ENT_conditionOccurrence}              
            WHERE
                stop_reason IS NULL              
            GROUP BY
                start_date              
            HAVING
                COUNT(*) > @groupByCount         
        )
