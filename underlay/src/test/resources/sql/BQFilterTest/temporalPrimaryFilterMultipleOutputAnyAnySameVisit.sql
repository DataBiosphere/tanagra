
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
                    person_id AS primaryEntityId,
                    date AS visitDate,
                    visit_occurrence_id AS visitOccurrenceId FROM${ENT_procedureOccurrence}                  
                WHERE
                    procedure IN (
                        SELECT
                            descendant                          
                        FROM
                            ${HAD_procedure_default}                          
                        WHERE
                            ancestor = @val0                          
                        UNION
                        ALL SELECT
                            @val1                     
                    )                  
                UNION
                ALL SELECT
                    person_id AS primaryEntityId,
                    start_date AS visitDate,
                    visit_occurrence_id AS visitOccurrenceId FROM${ENT_conditionOccurrence}                  
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
            ) AS firstCondition          
        JOIN
            (
                SELECT
                    person_id AS primaryEntityId,
                    date AS visitDate,
                    visit_occurrence_id AS visitOccurrenceId FROM${ENT_procedureOccurrence}                  
                WHERE
                    procedure IN (
                        SELECT
                            descendant                          
                        FROM
                            ${HAD_procedure_default}                          
                        WHERE
                            ancestor = @val4                          
                        UNION
                        ALL SELECT
                            @val5                     
                    )                  
                UNION
                ALL SELECT
                    person_id AS primaryEntityId,
                    start_date AS visitDate,
                    visit_occurrence_id AS visitOccurrenceId FROM${ENT_conditionOccurrence}                  
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
            ) AS secondCondition                  
                ON firstCondition.primaryEntityId = secondCondition.primaryEntityId                  
                AND firstCondition.visitDate = secondCondition.visitDate                  
                AND firstCondition.visitOccurrenceId = secondCondition.visitOccurrenceId             
            )
