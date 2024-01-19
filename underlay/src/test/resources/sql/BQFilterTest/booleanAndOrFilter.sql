
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        (
            concept_code != @val0         
        )          
        AND (
            REGEXP_CONTAINS(UPPER(T_TXT), UPPER(@val1))
        )
