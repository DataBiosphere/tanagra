
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (SELECT
            firstCondition.primaryEntityId          
        FROM
            (SELECT
                *              
            FROM
                (SELECT
                    person_id AS primaryEntityId, TIMESTAMP(start_date) AS visitDate, RANK() OVER (PARTITION                  
                BY
                    person_id                  
                ORDER BY
                    TIMESTAMP(start_date) ASC) AS orderRank FROM${ENT_conditionOccurrence}                  
                WHERE
                    (condition IN (SELECT
                        descendant                      
                    FROM
                        ${HAD_condition_default}                      
                    WHERE
                        ancestor = @val0                      
                    UNION
                    ALL SELECT
                        @val1))                      
                    AND (age_at_occurrence = @val2))                  
                WHERE
                    orderRank = 1) AS firstCondition                  
                JOIN
                    (SELECT
                        *                      
                    FROM
                        (SELECT
                            person_id AS primaryEntityId, TIMESTAMP(date) AS visitDate, RANK() OVER (PARTITION                          
                        BY
                            person_id                          
                        ORDER BY
                            TIMESTAMP(date) ASC) AS orderRank FROM${ENT_procedureOccurrence}                          
                        WHERE
                            (procedure IN (SELECT
                                descendant                              
                            FROM
                                ${HAD_procedure_default}                              
                            WHERE
                                ancestor = @val3                              
                            UNION
                            ALL SELECT
                                @val4))                              
                            AND (age_at_occurrence = @val5))                          
                        WHERE
                            orderRank = 1) AS secondCondition                                  
                                ON firstCondition.primaryEntityId = secondCondition.primaryEntityId                                  
                                AND TIMESTAMP_DIFF(secondCondition.visitDate, firstCondition.visitDate, DAY) >= 4)
