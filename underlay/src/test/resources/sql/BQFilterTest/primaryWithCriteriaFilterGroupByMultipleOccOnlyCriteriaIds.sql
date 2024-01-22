
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
                        @val,@val0                     
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
                        @val1,@val2                     
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
                        @val3,@val4                     
                    )             
            )          
        GROUP BY
            primary_id,
            group_by_0,
            group_by_1          
        HAVING
            COUNT(*) >= @groupByCountValue     
    )
