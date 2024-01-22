
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
                        condition = @val0                     
                    )                      
                    AND (
                        age_at_occurrence BETWEEN @val1 AND @val2                     
                    ))              
            GROUP BY
                primary_id,
                group_by_0              
            HAVING
                COUNT(*) = @groupByCountValue3             
            )
