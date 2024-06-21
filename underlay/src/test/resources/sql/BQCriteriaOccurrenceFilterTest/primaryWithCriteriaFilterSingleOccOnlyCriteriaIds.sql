
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
            condition IN (SELECT
                descendant              
            FROM
                ${HAD_condition_default}              
            WHERE
                ancestor IN (@val0, @val1)              
            UNION
            ALL SELECT
                @val2              
            UNION
            ALL SELECT
                @val3))
