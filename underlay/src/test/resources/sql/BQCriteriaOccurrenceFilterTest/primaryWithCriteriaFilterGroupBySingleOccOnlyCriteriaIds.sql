
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
                    person_id AS primary_id                  
                FROM
                    ${ENT_conditionOccurrence}                  
                WHERE
                    condition IN (
                        @val0,@val1                     
                    ))              
            GROUP BY
                primary_id              
            HAVING
                COUNT(*) > @groupByCountValue2             
            )
