
    SELECT
        c.name      
    FROM
        ${ENT_condition} AS c      
    WHERE
        REGEXP_CONTAINS(UPPER(c.T_TXT), UPPER(@val))
