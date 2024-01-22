
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
                    @val,@val0                 
                )              
            UNION
            ALL SELECT
                person_id AS primary_id              
            FROM
                ${ENT_procedureOccurrence}              
            WHERE
                source_criteria_id IN (
                    @val1,@val2                 
                )              
            UNION
            ALL SELECT
                person_id AS primary_id              
            FROM
                ${ENT_observationOccurrence}              
            WHERE
                source_criteria_id IN (
                    @val3,@val4                 
                )         
        )
