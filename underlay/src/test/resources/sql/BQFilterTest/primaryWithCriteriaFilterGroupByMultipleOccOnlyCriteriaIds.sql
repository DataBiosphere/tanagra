
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
                    age_at_occurrence AS group_by_0,
                    start_date AS group_by_1                  
                FROM
                    ${ENT_conditionOccurrence}                  
                WHERE
                    source_criteria_id IN (
                        @val0,@val1                     
                    )                  
                UNION
                ALL SELECT
                    person_id AS primary_id,
                    age_at_occurrence AS group_by_0,
                    date AS group_by_1                  
                FROM
                    ${ENT_observationOccurrence}                  
                WHERE
                    source_criteria_id IN (
                        @val2,@val3                     
                    )                  
                UNION
                ALL SELECT
                    person_id AS primary_id,
                    age_at_occurrence AS group_by_0,
                    date AS group_by_1                  
                FROM
                    ${ENT_procedureOccurrence}                  
                WHERE
                    source_criteria_id IN (
                        @val4,@val5                     
                    )             
            )          
        GROUP BY
            primary_id,
            group_by_0,
            group_by_1          
        HAVING
            COUNT(*) >= @groupByCountValue6     
    )
