
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
                    *                  
                FROM
                    (SELECT
                        primaryEntityId,
                        visitDate,
                        RANK() OVER (PARTITION                      
                    BY
                        primaryEntityId                      
                    ORDER BY
                        visitDate DESC) AS orderRank                      
                    FROM
                        (SELECT
                            person_id AS primaryEntityId,
                            date AS visitDate                          
                        FROM
                            `verily-tanagra-dev.cmssynpuf_index_010224`.ENT_procedureOccurrence                          
                        WHERE
                            id IN (
                                SELECT
                                    person_id AS primary_id                                  
                                FROM
                                    `verily-tanagra-dev.cmssynpuf_index_010224`.ENT_procedureOccurrence                                  
                                WHERE
                                    procedure IN (
                                        SELECT
                                            descendant                                          
                                        FROM
                                            `verily-tanagra-dev.cmssynpuf_index_010224`.HAD_procedure_default                                          
                                        WHERE
                                            ancestor = @val0                                          
                                        UNION
                                        ALL SELECT
                                            @val1                                     
                                    )                             
                            )                         
                        )                 
                )              
            WHERE
                orderRank = 1             
            ) AS firstCondition          
        JOIN
            (
                SELECT
                    *                  
                FROM
                    (SELECT
                        primaryEntityId,
                        visitDate,
                        RANK() OVER (PARTITION                      
                    BY
                        primaryEntityId                      
                    ORDER BY
                        visitDate DESC) AS orderRank                      
                    FROM
                        (SELECT
                            person_id AS primaryEntityId,
                            start_date AS visitDate                          
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
                )              
            WHERE
                orderRank = 1             
            ) AS secondCondition                  
                ON firstCondition.primaryEntityId = secondCondition.primaryEntityId                  
                AND TIMESTAMP_DIFF(firstCondition.visitDate,
            secondCondition.visitDate,
            DAY) >= 2)
