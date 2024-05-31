
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (
            SELECT
                firstCondition.primaryEntityId              
            FROM
                (SELECT
                    person_id AS primaryEntityId,
                    TIMESTAMP(start_date) AS visitDate,
                    IFNULL(visit_occurrence_id,
                    0) AS visitOccurrenceId FROM${ENT_conditionOccurrence}                  
                WHERE
                    condition IN (
                        SELECT
                            descendant                          
                        FROM
                            ${HAD_condition_default}                          
                        WHERE
                            ancestor = @val0                          
                        UNION
                        ALL SELECT
                            @val1                     
                    )             
            ) AS firstCondition          
        JOIN
            (
                SELECT
                    person_id AS primaryEntityId,
                    TIMESTAMP(start_date) AS visitDate,
                    IFNULL(visit_occurrence_id,
                    0) AS visitOccurrenceId FROM${ENT_conditionOccurrence}                  
                WHERE
                    condition IN (
                        SELECT
                            descendant                          
                        FROM
                            ${HAD_condition_default}                          
                        WHERE
                            ancestor = @val2                          
                        UNION
                        ALL SELECT
                            @val3                     
                    )             
            ) AS secondCondition                  
                ON firstCondition.primaryEntityId = secondCondition.primaryEntityId                  
                AND firstCondition.visitDate = secondCondition.visitDate                  
                AND firstCondition.visitOccurrenceId = secondCondition.visitOccurrenceId             
            )
