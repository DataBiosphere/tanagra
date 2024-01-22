
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (
            SELECT
                person_id AS primary_id              
            FROM
                ${ENT_conditionOccurrence}              
            WHERE
                (
                    source_criteria_id = @val0                 
                )                  
                AND (
                    age_at_occurrence BETWEEN @val1 AND @val2                 
                )              
            UNION
            ALL SELECT
                person_id AS primary_id              
            FROM
                ${ENT_observationOccurrence}              
            WHERE
                (
                    source_criteria_id = @val3                 
                )                  
                AND (
                    age_at_occurrence BETWEEN @val4 AND @val5                 
                )              
            UNION
            ALL SELECT
                person_id AS primary_id              
            FROM
                ${ENT_procedureOccurrence}              
            WHERE
                (
                    source_criteria_id = @val6                 
                )                  
                AND (
                    age_at_occurrence BETWEEN @val7 AND @val8                 
                )         
        )
