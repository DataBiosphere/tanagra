
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (SELECT
            primary_id          
        FROM
            (SELECT
                primary_id, group_by_0, group_by_1              
            FROM
                (SELECT
                    person_id AS primary_id, start_date AS group_by_0, condition AS group_by_1                  
                FROM
                    ${ENT_conditionOccurrence}                  
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
                    AND (age_at_occurrence BETWEEN @val2 AND @val3))                  
                GROUP BY
                    primary_id, group_by_0, group_by_1)                  
                GROUP BY
                    primary_id                  
                HAVING
                    COUNT(*) = @groupByCountValue4)
