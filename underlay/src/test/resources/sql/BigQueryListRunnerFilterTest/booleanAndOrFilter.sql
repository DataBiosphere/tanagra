
    SELECT
        c.name      
    FROM
        ${ENT_condition} AS c      
    WHERE
        c.concept_code != @val          
        AND REGEXP_CONTAINS(UPPER(c.T_TXT), UPPER(@val0))
