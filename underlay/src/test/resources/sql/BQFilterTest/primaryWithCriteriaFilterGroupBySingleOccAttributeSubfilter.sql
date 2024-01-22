
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (
            SELECT
                primary_id              
            FROM
                (SELECT
                    person_id AS primary_id,
                    start_date AS group_by_0                  
                FROM
                    ${ENT_conditionOccurrence}                  
                WHERE
                    (
                        condition = @val                     
                    )                      
                    AND (
                        age_at_occurrence BETWEEN @val0 AND @val1                     
                    ))              
            GROUP BY
                primary_id,
                group_by_0              
            HAVING
                COUNT(*) = @groupByCountValue             
            )
