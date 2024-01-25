
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
                        SELECT
                            descendant                          
                        FROM
                            ${HAD_icd9cm_default}                          
                        WHERE
                            ancestor IN (
                                @val0,@val1                             
                            )                          
                        UNION
                        ALL SELECT
                            @val2                          
                        UNION
                        ALL SELECT
                            @val3                     
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
                        SELECT
                            descendant                          
                        FROM
                            ${HAD_icd9cm_default}                          
                        WHERE
                            ancestor IN (
                                @val4,@val5                             
                            )                          
                        UNION
                        ALL SELECT
                            @val6                          
                        UNION
                        ALL SELECT
                            @val7                     
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
                        SELECT
                            descendant                          
                        FROM
                            ${HAD_icd9cm_default}                          
                        WHERE
                            ancestor IN (
                                @val8,@val9                             
                            )                          
                        UNION
                        ALL SELECT
                            @val10                          
                        UNION
                        ALL SELECT
                            @val11                     
                    )             
            )          
        GROUP BY
            primary_id,
            group_by_0,
            group_by_1          
        HAVING
            COUNT(*) >= @groupByCountValue12     
    )
