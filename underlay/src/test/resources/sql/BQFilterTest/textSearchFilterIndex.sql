
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        REGEXP_CONTAINS(UPPER(T_TXT), UPPER(@val))
