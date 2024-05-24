
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
                    primaryEntityId,
                    visitDate,
                    visitOccurrenceId                  
                FROM
                    (SELECT
                        person_id AS primaryEntityId,
                        start_date AS visitDate,
                        visit_occurrence_id AS visitOccurrenceId                      
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_010224`.ENT_conditionOccurrence                      
                    WHERE
                        id IN (
                            SELECT
                                person_id AS primary_id                              
                            FROM
                                `verily-tanagra-dev.cmssynpuf_index_010224`.ENT_conditionOccurrence                              
                            WHERE
                                condition IN (
                                    SELECT
                                        descendant                                      
                                    FROM
                                        `verily-tanagra-dev.cmssynpuf_index_010224`.HAD_condition_default                                      
                                    WHERE
                                        ancestor = @val0                                      
                                    UNION
                                    ALL SELECT
                                        @val1                                 
                                )                         
                        )                     
                    )             
            ) AS firstCondition          
        JOIN
            (
                SELECT
                    primaryEntityId,
                    visitDate,
                    visitOccurrenceId                  
                FROM
                    (SELECT
                        person_id AS primaryEntityId,
                        start_date AS visitDate,
                        visit_occurrence_id AS visitOccurrenceId                      
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_010224`.ENT_conditionOccurrence                      
                    WHERE
                        id IN (
                            SELECT
                                person_id AS primary_id                              
                            FROM
                                `verily-tanagra-dev.cmssynpuf_index_010224`.ENT_conditionOccurrence                              
                            WHERE
                                condition IN (
                                    SELECT
                                        descendant                                      
                                    FROM
                                        `verily-tanagra-dev.cmssynpuf_index_010224`.HAD_condition_default                                      
                                    WHERE
                                        ancestor = @val2                                      
                                    UNION
                                    ALL SELECT
                                        @val3                                 
                                )                         
                        )                     
                    )             
            ) AS secondCondition                  
                ON firstCondition.primaryEntityId = secondCondition.primaryEntityId                  
                AND firstCondition.visitDate = secondCondition.visitDate                  
                AND firstCondition.visitOccurrenceId = secondCondition.visitOccurrenceId             
            )
