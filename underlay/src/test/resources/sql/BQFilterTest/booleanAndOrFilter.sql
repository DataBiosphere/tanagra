
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        (
            concept_code != @val         
        )          
        AND (
            REGEXP_CONTAINS(UPPER(T_TXT), UPPER(@val0))         
        )
