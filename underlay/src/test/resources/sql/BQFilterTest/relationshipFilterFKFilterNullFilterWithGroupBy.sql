
    SELECT
        year_of_birth      
    FROM
        ${ENT_person}      
    WHERE
        id IN (
            SELECT
                person_id              
            FROM
                (SELECT
                    person_id                  
                FROM
                    ${ENT_conditionOccurrence}                  
                GROUP BY
                    person_id,
                    start_date)              
            GROUP BY
                person_id              
            HAVING
                COUNT(*) > @groupByCount0             
            )
