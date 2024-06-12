
    SELECT
        id      
    FROM
        ${ENT_person}      
    WHERE
        id IN (SELECT
            person_id AS primary_id          
        FROM
            ${ENT_conditionOccurrence}          
        WHERE
            (source_criteria_id IN (SELECT
                descendant              
            FROM
                ${HAD_icd9cm_default}              
            WHERE
                ancestor = @val0              
            UNION
            ALL SELECT
                @val1))              
            AND (age_at_occurrence BETWEEN @val2 AND @val3)          
        UNION
        ALL SELECT
            person_id AS primary_id          
        FROM
            ${ENT_observationOccurrence}          
        WHERE
            (source_criteria_id IN (SELECT
                descendant              
            FROM
                ${HAD_icd9cm_default}              
            WHERE
                ancestor = @val4              
            UNION
            ALL SELECT
                @val5))              
            AND (age_at_occurrence BETWEEN @val6 AND @val7)          
        UNION
        ALL SELECT
            person_id AS primary_id          
        FROM
            ${ENT_procedureOccurrence}          
        WHERE
            (source_criteria_id IN (SELECT
                descendant              
            FROM
                ${HAD_icd9cm_default}              
            WHERE
                ancestor = @val8              
            UNION
            ALL SELECT
                @val9))              
            AND (age_at_occurrence BETWEEN @val10 AND @val11))
