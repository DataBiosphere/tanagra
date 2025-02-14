
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        NOT EXISTS (SELECT
            *          
        FROM
            UNNEST(['foo', 'bar', 'baz', vocabulary]))
