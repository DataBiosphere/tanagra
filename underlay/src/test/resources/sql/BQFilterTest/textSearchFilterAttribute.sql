
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        REGEXP_CONTAINS(UPPER(name), UPPER(@val))
