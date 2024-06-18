
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
                source_criteria_id IN (
                    @val0,@val1                 
                )              
            UNION
            ALL SELECT
                person_id AS primary_id              
            FROM
                ${ENT_observationOccurrence}              
            WHERE
                source_criteria_id IN (
                    @val2,@val3                 
                )              
            UNION
            ALL SELECT
                person_id AS primary_id              
            FROM
                ${ENT_procedureOccurrence}              
            WHERE
                source_criteria_id IN (
                    @val4,@val5                 
                )         
        )
