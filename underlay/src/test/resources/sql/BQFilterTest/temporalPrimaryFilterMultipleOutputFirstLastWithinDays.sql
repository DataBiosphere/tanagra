
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
                        visitDate ASC) AS orderRank                      
                    FROM
                        (SELECT
                            person_id AS primaryEntityId,
                            start_date AS visitDate FROM${ENT_conditionOccurrence}                          
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
                        UNION
                        ALL SELECT
                            person_id AS primaryEntityId,
                            start_date AS visitDate FROM${ENT_conditionOccurrence}                          
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
                        start_date AS visitDate FROM${ENT_conditionOccurrence}                      
                    WHERE
                        condition IN (
                            SELECT
                                descendant                              
                            FROM
                                ${HAD_condition_default}                              
                            WHERE
                                ancestor = @val4                              
                            UNION
                            ALL SELECT
                                @val5                         
                        )                      
                    UNION
                    ALL SELECT
                        person_id AS primaryEntityId,
                        start_date AS visitDate FROM${ENT_conditionOccurrence}                      
                    WHERE
                        condition IN (
                            SELECT
                                descendant                              
                            FROM
                                ${HAD_condition_default}                              
                            WHERE
                                ancestor = @val6                              
                            UNION
                            ALL SELECT
                                @val7                         
                        )                 
                )             
            )          
        WHERE
            orderRank = 1     
    ) AS secondCondition          
        ON firstCondition.primaryEntityId = secondCondition.primaryEntityId          
        AND ABS(TIMESTAMP_DIFF(firstCondition.visitDate,
    secondCondition.visitDate,
    DAY)) <= 90 
)
