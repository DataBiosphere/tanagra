
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
                    ${ENT_observationOccurrence}                  
                WHERE
                    (
                        source_criteria_id = @val                     
                    )                      
                    AND (
                        age_at_occurrence BETWEEN @val0 AND @val1                     
                    )                  
                UNION
                ALL SELECT
                    person_id AS primary_id                  
                FROM
                    ${ENT_procedureOccurrence}                  
                WHERE
                    (
                        source_criteria_id = @val2                     
                    )                      
                    AND (
                        age_at_occurrence BETWEEN @val3 AND @val4                     
                    )                  
                UNION
                ALL SELECT
                    person_id AS primary_id                  
                FROM
                    ${ENT_conditionOccurrence}                  
                WHERE
                    (
                        source_criteria_id = @val5                     
                    )                      
                    AND (
                        age_at_occurrence BETWEEN @val6 AND @val7                     
                    )             
            )          
        GROUP BY
            primary_id          
        HAVING
            COUNT(*) < @groupByCountValue     
    )
