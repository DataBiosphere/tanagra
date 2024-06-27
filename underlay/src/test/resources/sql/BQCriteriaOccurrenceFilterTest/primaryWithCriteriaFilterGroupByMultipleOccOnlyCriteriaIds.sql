
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
                    person_id AS primary_id, start_date AS group_by_0, source_criteria_id AS group_by_1                  
                FROM
                    ${ENT_conditionOccurrence}                  
                WHERE
                    source_criteria_id IN (SELECT
                        descendant                      
                    FROM
                        ${HAD_icd9cm_default}                      
                    WHERE
                        ancestor IN (@val0, @val1)                      
                    UNION
                    ALL SELECT
                        @val2                      
                    UNION
                    ALL SELECT
                        @val3)                  
                UNION
                ALL SELECT
                    person_id AS primary_id, date AS group_by_0, source_criteria_id AS group_by_1                  
                FROM
                    ${ENT_observationOccurrence}                  
                WHERE
                    source_criteria_id IN (SELECT
                        descendant                      
                    FROM
                        ${HAD_icd9cm_default}                      
                    WHERE
                        ancestor IN (@val4, @val5)                      
                    UNION
                    ALL SELECT
                        @val6                      
                    UNION
                    ALL SELECT
                        @val7)                  
                UNION
                ALL SELECT
                    person_id AS primary_id, date AS group_by_0, source_criteria_id AS group_by_1                  
                FROM
                    ${ENT_procedureOccurrence}                  
                WHERE
                    source_criteria_id IN (SELECT
                        descendant                      
                    FROM
                        ${HAD_icd9cm_default}                      
                    WHERE
                        ancestor IN (@val8, @val9)                      
                    UNION
                    ALL SELECT
                        @val10                      
                    UNION
                    ALL SELECT
                        @val11))                  
                GROUP BY
                    primary_id, group_by_0, group_by_1)                  
                GROUP BY
                    primary_id                  
                HAVING
                    COUNT(*) >= @groupByCountValue12)
