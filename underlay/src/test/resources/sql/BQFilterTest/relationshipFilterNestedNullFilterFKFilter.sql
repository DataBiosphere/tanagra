
    SELECT
        start_date      
    FROM
        ${ENT_conditionOccurrence}      
    WHERE
        person_id IN (
            SELECT
                person_id              
            FROM
                `verily-tanagra-dev.sd20230331_index_010224`.ENT_bmi         
        )
