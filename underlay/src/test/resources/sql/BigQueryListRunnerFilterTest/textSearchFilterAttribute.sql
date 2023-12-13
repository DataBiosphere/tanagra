
    SELECT
        c.name      
    FROM
        ${ENT_condition} AS c      
    WHERE
        REGEXP_CONTAINS(UPPER(c.name), UPPER(@val))
